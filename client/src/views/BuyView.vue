<script setup>
import { computed, onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { ElMessage, ElMessageBox } from 'element-plus';
import { clientApi } from '../api/client.js';
import { useAuth } from '../composables/useAuth.js';
import { useCredit } from '../composables/useCredit.js';
import { EXPLORER_ADDRESS_URL, EXPLORER_TX_URL, DOC_TOKEN_ADDRESS, MAX_PURCHASE_QUANTITY } from '../config.js';
import { formatWei } from '../utils/format.js';
import { isQuantity } from '../utils/validate.js';
import { mulWei, mulDivWei, tokensToWei, WEI_PER_TOKEN } from '../utils/wei.js';
import {
  burnDocToken,
  docTokenBalanceOf,
  docTokenEthPerToken,
  ethBalanceOf,
  buyDocTokenWithEth,
  waitForTx,
  mapWalletError,
} from '../wallet/wallet.js';

const { t } = useI18n();
const { address } = useAuth();
const { credit, refresh } = useCredit();

const quantity = ref(null); // el-input-number，null = 未填
const tokenBalance = ref(null); // 钱包 DocToken 余额（wei 字符串）
const tokenLoading = ref(false);
const step = ref(''); // '' | 'sending' | 'waiting'
const lastTx = ref('');

// 链上销毁金额 = 数量 × 单价（BigInt 运算，防精度丢失）；输入不合法时为 null
const amountWei = computed(() => {
  if (!isQuantity(quantity.value, MAX_PURCHASE_QUANTITY) || !credit.value) return null;
  try {
    return mulWei(quantity.value, credit.value.unitPriceDocToken);
  } catch {
    return null;
  }
});

const submitting = computed(() => step.value !== '');

// ---- ETH 兑换 DocToken ----
const tokenQty = ref(null); // 兑换 DocToken 数量，null = 未填
const ethRate = ref(null); // 链上汇率 wei 字符串（1 枚 DocToken = X wei ETH）
const ethRateLoading = ref(false);
const ethBalance = ref(null); // 钱包 ETH 余额 wei 字符串
const ethBalanceLoading = ref(false);
const ethStep = ref(''); // '' | 'sending' | 'waiting'
const ethLastTx = ref('');
const ethSubmitting = computed(() => ethStep.value !== '');

// 应付 ETH = 数量(枚) × 汇率（与合约 buyTokens 算法一致：tokenAmount * ethPerToken / 10^18，
// tokenAmount 为 18 位小数 wei、汇率为每枚 DocToken 对应的 ETH wei）；不合法时为 null
const ethCostWei = computed(() => {
  if (!isQuantity(tokenQty.value, MAX_PURCHASE_QUANTITY) || !ethRate.value) return null;
  try {
    return mulDivWei(tokensToWei(tokenQty.value), ethRate.value, WEI_PER_TOKEN);
  } catch {
    return null; // 汇率极端值导致溢出：视为不合法
  }
});

/** 读取链上汇率（eth_call ethPerToken） */
async function loadEthRate() {
  ethRateLoading.value = true;
  try {
    ethRate.value = await docTokenEthPerToken();
  } catch {
    ethRate.value = null;
  } finally {
    ethRateLoading.value = false;
  }
}

/** 查询钱包 ETH 余额（eth_getBalance） */
async function loadEthBalance() {
  if (!address.value) return;
  ethBalanceLoading.value = true;
  try {
    ethBalance.value = await ethBalanceOf(address.value);
  } catch {
    ethBalance.value = null;
  } finally {
    ethBalanceLoading.value = false;
  }
}

/**
 * ETH 兑换 DocToken：确认 → 钱包发 buyTokens 交易（msg.value = 应付 ETH，多付合约找零）
 * → 等链上确认 → 刷新汇率 / ETH 余额 / DocToken 余额
 */
async function doExchange() {
  if (ethSubmitting.value) return;
  if (!ethCostWei.value) {
    ElMessage.warning(ethRate.value ? t('buy.tokenQtyInvalid') : t('buy.rateUnknown'));
    return;
  }
  // ETH 余额充足性预检（BigInt 比较）
  if (ethBalance.value !== null && BigInt(ethBalance.value) < BigInt(ethCostWei.value)) {
    ElMessage.warning(
      t('buy.insufficientEth', {
        need: formatWei(ethCostWei.value),
        have: formatWei(ethBalance.value),
      })
    );
    return;
  }
  try {
    await ElMessageBox.confirm(
      t('buy.ethConfirm', { n: tokenQty.value, cost: formatWei(ethCostWei.value) }),
      t('common.tip'),
      {
        type: 'warning',
        confirmButtonText: t('common.confirm'),
        cancelButtonText: t('common.cancel'),
      }
    );
  } catch {
    return;
  }

  try {
    ethStep.value = 'sending';
    const txHash = await buyDocTokenWithEth(tokensToWei(tokenQty.value), ethCostWei.value);
    ethLastTx.value = txHash;
    ethStep.value = 'waiting';
    const receipt = await waitForTx(txHash);
    if (!receipt || receipt.status !== '0x1') {
      throw new Error('TX_FAILED');
    }
    ElMessage.success(t('buy.ethSuccess', { n: tokenQty.value }));
    tokenQty.value = null;
    loadEthRate();
    loadEthBalance();
    loadTokenBalance();
  } catch (e) {
    if (e.message === 'TX_FAILED') {
      ElMessage.error(t('buy.txFailed'));
    } else {
      ElMessage.error(mapWalletError(e, t));
    }
  } finally {
    ethStep.value = '';
  }
}

/** 查询钱包 DocToken 余额（eth_call balanceOf） */
async function loadTokenBalance() {
  if (!address.value) return;
  tokenLoading.value = true;
  try {
    tokenBalance.value = await docTokenBalanceOf(address.value);
  } catch {
    tokenBalance.value = null; // 查询失败不阻塞购买流程，提交前若查不到余额则跳过余额校验
  } finally {
    tokenLoading.value = false;
  }
}

/**
 * 购买次数：确认 → 钱包链上销毁 DocToken（打进 0 地址） → 等链上确认 → 自动提交 txHash 完成购买
 */
async function doBuy() {
  if (submitting.value) return;
  if (!amountWei.value) {
    ElMessage.warning(t('buy.quantityInvalid'));
    return;
  }
  // 余额充足性预检（BigInt 比较）
  if (tokenBalance.value !== null && BigInt(tokenBalance.value) < BigInt(amountWei.value)) {
    ElMessage.warning(
      t('buy.insufficient', {
        need: formatWei(amountWei.value),
        have: formatWei(tokenBalance.value),
      })
    );
    return;
  }
  try {
    await ElMessageBox.confirm(
      t('buy.confirm', { amount: formatWei(amountWei.value) }),
      t('common.tip'),
      {
        type: 'warning',
        confirmButtonText: t('common.confirm'),
        cancelButtonText: t('common.cancel'),
      }
    );
  } catch {
    return;
  }

  try {
    step.value = 'sending';
    const txHash = await burnDocToken(amountWei.value);
    lastTx.value = txHash;
    step.value = 'waiting';
    const receipt = await waitForTx(txHash);
    if (!receipt || receipt.status !== '0x1') {
      throw new Error('TX_FAILED');
    }
    await clientApi.purchase(quantity.value, txHash);
    ElMessage.success(t('buy.success', { n: quantity.value }));
    quantity.value = null;
    await refresh();
    loadTokenBalance();
  } catch (e) {
    if (e.isBusinessError) {
      // 后端业务错误：409 = txHash 已使用过（幂等），其余展示后端文案
      if (e.code === 409) ElMessage.warning(t('buy.txUsed'));
      else ElMessage.error(e.message);
    } else if (e.message === 'TX_FAILED') {
      ElMessage.error(t('buy.txFailed'));
    } else {
      ElMessage.error(mapWalletError(e, t));
    }
  } finally {
    step.value = '';
  }
}

onMounted(() => {
  loadTokenBalance();
  loadEthRate();
  loadEthBalance();
});
</script>

<template>
  <div>
    <el-card class="credit-card">
      <template #header>
        <div class="card-title">{{ t('buy.myCredit') }}</div>
      </template>
      <el-row :gutter="24">
        <el-col :span="8">
          <div class="label">{{ t('buy.balance') }}</div>
          <div class="big-number">{{ credit ? credit.balance : '-' }}</div>
        </el-col>
        <el-col :span="8">
          <div class="label">{{ t('buy.unitPrice') }}</div>
          <div class="value">
            {{ credit ? formatWei(credit.unitPriceDocToken) : '-' }}
            <span class="unit">{{ t('buy.unitPriceUnit') }}</span>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="label">
            {{ t('buy.walletBalance') }}
            <el-icon v-if="tokenLoading" class="is-loading" style="vertical-align: -2px"><Loading /></el-icon>
          </div>
          <div class="value">
            {{ tokenBalance !== null ? formatWei(tokenBalance) : '-' }}
            <span class="unit">DocToken</span>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <el-card class="exchange-card">
      <template #header>
        <div class="card-title">{{ t('buy.ethExchange') }}</div>
      </template>
      <el-row :gutter="24">
        <el-col :span="8">
          <div class="label">
            {{ t('buy.ethRate') }}
            <el-button size="small" text type="primary" :loading="ethRateLoading" @click="loadEthRate">{{ t('buy.refresh') }}</el-button>
          </div>
          <div class="value">
            <template v-if="ethRate !== null">
              {{ formatWei(ethRate) }} <span class="unit">ETH / DocToken</span>
            </template>
            <template v-else>-</template>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="label">
            {{ t('buy.ethBalance') }}
            <el-button size="small" text type="primary" :loading="ethBalanceLoading" @click="loadEthBalance">{{ t('buy.refresh') }}</el-button>
          </div>
          <div class="value">
            {{ ethBalance !== null ? formatWei(ethBalance) : '-' }}
            <span class="unit">ETH</span>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="label">{{ t('buy.ethCost') }}</div>
          <div class="value">
            <template v-if="ethCostWei !== null">
              {{ formatWei(ethCostWei) }} <span class="unit">ETH</span>
            </template>
            <template v-else>-</template>
          </div>
        </el-col>
      </el-row>
      <el-divider />
      <el-form label-width="140px">
        <el-form-item :label="t('buy.tokenQuantity')">
          <el-input-number
            v-model="tokenQty"
            :min="1"
            :max="MAX_PURCHASE_QUANTITY"
            :step="1"
            :disabled="ethSubmitting"
            style="width: 220px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="ethSubmitting" @click="doExchange">
            {{ ethSubmitting ? (ethStep === 'sending' ? t('buy.sending') : t('buy.waiting')) : t('buy.ethSubmit') }}
          </el-button>
          <a v-if="ethLastTx" class="mono" :href="EXPLORER_TX_URL + ethLastTx" target="_blank" rel="noopener">
            {{ ethLastTx.slice(0, 10) }}...
          </a>
        </el-form-item>
      </el-form>
      <el-alert :title="t('buy.ethHint')" type="info" :closable="false">
        <template #default>
          {{ t('buy.ethHint') }}
        </template>
      </el-alert>
    </el-card>

    <el-card class="buy-card">
      <template #header>
        <div class="card-title">{{ t('buy.purchase') }}</div>
      </template>
      <el-form label-width="140px">
        <el-form-item :label="t('buy.quantity')">
          <el-input-number
            v-model="quantity"
            :min="1"
            :max="MAX_PURCHASE_QUANTITY"
            :step="1"
            :disabled="submitting"
            style="width: 220px"
          />
        </el-form-item>
        <el-form-item :label="t('buy.amount')">
          <span v-if="amountWei !== null" class="value">
            {{ formatWei(amountWei) }} <span class="unit">{{ t('buy.amountUnit') }}</span>
          </span>
          <span v-else>-</span>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="doBuy">
            {{ submitting ? (step === 'sending' ? t('buy.sending') : t('buy.waiting')) : t('buy.submit') }}
          </el-button>
          <a v-if="lastTx" class="mono" :href="EXPLORER_TX_URL + lastTx" target="_blank" rel="noopener">
            {{ lastTx.slice(0, 10) }}...
          </a>
        </el-form-item>
      </el-form>
      <el-alert :title="t('buy.tokenHint')" type="info" :closable="false">
        <template #default>
          {{ t('buy.tokenHint') }}
          <a :href="EXPLORER_ADDRESS_URL + DOC_TOKEN_ADDRESS" target="_blank" rel="noopener">{{ t('buy.tokenContract') }}</a>
        </template>
      </el-alert>
    </el-card>
  </div>
</template>

<style scoped>
.credit-card { margin-bottom: 16px; }
.exchange-card { margin-bottom: 16px; }
.card-title { font-size: 15px; font-weight: 600; }
.label { color: #64748b; font-size: 13px; margin-bottom: 6px; }
.big-number { font-size: 28px; font-weight: 700; color: #2563eb; }
.value { font-size: 15px; font-weight: 600; }
.unit { font-size: 12px; color: #94a3b8; font-weight: normal; margin-left: 4px; }
.mono { font-family: Consolas, Menlo, monospace; font-size: 12px; }
</style>
