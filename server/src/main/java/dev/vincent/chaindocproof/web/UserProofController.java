package dev.vincent.chaindocproof.web;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import dev.vincent.chaindocproof.model.ProofRecord;
import dev.vincent.chaindocproof.model.dto.ProofVo;
import dev.vincent.chaindocproof.service.ProofService;
import dev.vincent.chaindocproof.support.BizException;
import dev.vincent.chaindocproof.support.Result;

/**
 * 用户端存证：上传 → 存证 → 校验 → 列表
 */
@RestController
@RequestMapping("/api/user/proofs")
public class UserProofController {

    private static final Logger log = LoggerFactory.getLogger(UserProofController.class);

    private final ProofService proofService;

    public UserProofController(ProofService proofService) {
        this.proofService = proofService;
    }

    /**
     * 上传文件：只计算 SHA-256，文件不落盘；同一用户受上传冷却限制
     */
    @PostMapping("/upload")
    public Result<ProofVo> upload(HttpServletRequest request, @RequestParam("file") MultipartFile file) {
        String address = (String) request.getAttribute("userAddress");
        // 只打印文件名与大小，不打印文件内容
        log.debug("upload 入参: address={}, file={}, size={}",
                address, file.getOriginalFilename(), file.getSize());
        ProofRecord record;
        try {
            record = proofService.upload(address, file.getOriginalFilename(), file.getSize(), file.getInputStream());
        } catch (IOException e) {
            throw new BizException(400, "读取上传文件失败: " + e.getMessage());
        }
        return Result.ok(ProofVo.from(record));
    }

    /** 存证：扣 1 次次数，把 hash + 时间戳上链 */
    @PostMapping("/{id}/store")
    public Result<ProofVo> store(HttpServletRequest request, @PathVariable("id") long id) {
        String address = (String) request.getAttribute("userAddress");
        log.debug("store 入参: address={}, id={}", address, id);
        return Result.ok(ProofVo.from(proofService.store(address, id)));
    }

    /** 校验：重新上传文件，与链上 hash 对比（无论结果如何均扣 1 次次数） */
    @PostMapping("/{id}/verify")
    public Result<Boolean> verify(HttpServletRequest request, @PathVariable("id") long id,
                                  @RequestParam("file") MultipartFile file) {
        String address = (String) request.getAttribute("userAddress");
        log.debug("verify 入参: address={}, id={}, file={}",
                address, id, file.getOriginalFilename());
        boolean match;
        try {
            match = proofService.verify(address, id, file.getInputStream());
        } catch (IOException e) {
            throw new BizException(400, "读取上传文件失败: " + e.getMessage());
        }
        return Result.ok(match);
    }

    /** 我的存证记录列表 */
    @GetMapping
    public Result<List<ProofVo>> list(HttpServletRequest request) {
        String address = (String) request.getAttribute("userAddress");
        log.debug("list 入参: address={}", address);
        List<ProofVo> voList = proofService.listByUser(address).stream()
                .map(ProofVo::from)
                .collect(Collectors.toList());
        return Result.ok(voList);
    }
}
