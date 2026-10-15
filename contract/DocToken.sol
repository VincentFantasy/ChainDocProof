// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

/**
 * DocToken：链上资产存证服务的凭证代币（ERC20）
 * - 可用 ETH 按固定汇率指定数量兑换（buyTokens）
 * - 管理端（owner）可发放到指定钱包（mint）
 * - 购买次数时用户 burn 销毁自己的 DocToken（打进 0 地址）
 */
contract DocToken {
    string public name = "DocToken";
    string public symbol = "DOC";
    uint8 public decimals = 18;

    uint256 public totalSupply;
    mapping(address => uint256) public balanceOf;
    mapping(address => mapping(address => uint256)) public allowance;

    /// 管理端地址
    address public owner;
    /// 汇率：1 枚 DocToken 对应的 ETH（wei）
    uint256 public ethPerToken;
    /// 累计 ETH 兑换次数
    uint256 public buyCount;

    event Transfer(address indexed from, address indexed to, uint256 value);
    event Approval(address indexed owner, address indexed spender, uint256 value);
    /// ETH 兑换成功（ethAmount 为实际消耗的 ETH，不含找零）
    event BuyTokens(address indexed buyer, uint256 ethAmount, uint256 tokenAmount);
    /// 管理端发放
    event Mint(address indexed to, uint256 amount);
    /// 汇率更新
    event ExchangeRateUpdated(uint256 ethPerToken);

    modifier onlyOwner() {
        require(msg.sender == owner, "only owner");
        _;
    }

/**
 * 构造函数：显式指定管理端地址（只有管理端能 mint 发放 / setEthPerToken 调汇率），
 * 不要求部署者与 adminAddr 是同一地址；初始汇率 0.0001 ETH = 1 DocToken
 * 部署后管理端需先 mint 一批库存到本合约地址，供 ETH 兑换使用
 */
constructor(address adminAddr) {
    require(adminAddr != address(0), "invalid admin");
    owner = adminAddr;
    ethPerToken = 0.0001 ether;
}

    /** 管理端发放 DocToken 到指定地址（用于初始化库存、发放给指定钱包） */
    function mint(address to, uint256 amount) external onlyOwner {
        require(to != address(0) && amount > 0, "invalid params");
        totalSupply += amount;
        balanceOf[to] += amount;
        emit Transfer(address(0), to, amount);
        emit Mint(to, amount);
    }

    /**
     * 用 ETH 兑换 DocToken（固定汇率，调用方指定兑换数量）
     * tokenAmount 为 wei（18 位小数），ethPerToken 为每枚 DocToken 对应的 ETH（wei），
     * 故应付 ETH = tokenAmount * ethPerToken / 10^18；msg.value 不足时 revert，多付的 ETH 找零退回
     */
    function buyTokens(uint256 tokenAmount) external payable {
        require(tokenAmount > 0, "invalid amount");
        require(ethPerToken > 0, "rate not set");
        uint256 cost = tokenAmount * ethPerToken / 10 ** 18;
        require(balanceOf[address(this)] >= tokenAmount, "insufficient stock");
        require(msg.value >= cost, "insufficient ETH");
        balanceOf[address(this)] -= tokenAmount;
        balanceOf[msg.sender] += tokenAmount;
        buyCount += 1;
        if (msg.value > cost) {
            // 找零退回（call 替代已废弃的 transfer，必须显式检查成功）
            (bool ok, ) = msg.sender.call{value: msg.value - cost}("");
            require(ok, "refund failed");
        }
        emit Transfer(address(this), msg.sender, tokenAmount);
        emit BuyTokens(msg.sender, cost, tokenAmount);
    }

    /** 管理端调整 ETH 兑换汇率（wei/枚） */
    function setEthPerToken(uint256 value) external onlyOwner {
        require(value > 0, "invalid rate");
        ethPerToken = value;
        emit ExchangeRateUpdated(value);
    }

    /** 转账 */
    function transfer(address to, uint256 amount) external returns (bool) {
        require(to != address(0), "invalid to");
        require(balanceOf[msg.sender] >= amount, "insufficient balance");
        balanceOf[msg.sender] -= amount;
        balanceOf[to] += amount;
        emit Transfer(msg.sender, to, amount);
        return true;
    }

    /** 授权 */
    function approve(address spender, uint256 amount) external returns (bool) {
        allowance[msg.sender][spender] = amount;
        emit Approval(msg.sender, spender, amount);
        return true;
    }

    /**
     * 代付转账（含授权校验）
     * to 为 0 地址时视为销毁（totalSupply 相应减少）
     */
    function transferFrom(address from, address to, uint256 amount) external returns (bool) {
        require(from != address(0), "invalid from");
        require(balanceOf[from] >= amount, "insufficient balance");
        if (from != msg.sender) {
            uint256 a = allowance[from][msg.sender];
            require(a >= amount, "insufficient allowance");
            allowance[from][msg.sender] = a - amount;
        }
        if (to == address(0)) {
            totalSupply -= amount;
        } else {
            balanceOf[from] -= amount;
            balanceOf[to] += amount;
        }
        emit Transfer(from, to, amount);
        return true;
    }

    /**
     * 销毁调用方自己的代币（购买次数时用，打进 0 地址）：
     * 从 msg.sender 扣除 amount 并减少总供应量
     */
    function burn(uint256 amount) external {
        require(balanceOf[msg.sender] >= amount, "insufficient balance");
        balanceOf[msg.sender] -= amount;
        totalSupply -= amount;
        emit Transfer(msg.sender, address(0), amount);
    }
}
