// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

/**
 * DocProof：文件存证合约
 * 把文件 SHA-256 与时间戳上链，之后可验证文件是否被篡改
 * （DocToken 在购买次数时由用户 burn 销毁，本合约不再消耗代币）
 */
contract DocProof {
    struct Proof {
        uint256 index; // 存证序号（从 0 开始）
        bytes32 hash; // 文件 SHA-256
        uint256 timestamp; // 文件上传时间戳（秒）
        address owner; // 存证请求方
        uint256 blockTime; // 上链区块时间戳（秒）
    }

    /// 存证列表
    Proof[] public proofs;
    /// hash -> 存证序号+1（0 表示不存在，避免与序号 0 冲突）
    mapping(bytes32 => uint256) public hashIndex;

    event ProofStored(uint256 indexed index, bytes32 hash, uint256 timestamp, address indexed owner);

    /**
     * 存证：把 hash + 时间戳上链
     * （不消耗 DocToken，次数由后端在调用前扣减）
     *
     * @param hash      文件 SHA-256
     * @param timestamp 文件上传时间戳（秒）
     */
    function storeProof(bytes32 hash, uint256 timestamp) external {
        require(hash != bytes32(0), "empty hash");
        uint256 index = proofs.length;
        proofs.push(Proof({index: index, hash: hash, timestamp: timestamp, owner: msg.sender, blockTime: block.timestamp}));
        if (hashIndex[hash] == 0) {
            hashIndex[hash] = index + 1;
        }
        emit ProofStored(index, hash, timestamp, msg.sender);
    }

    /** 存证总数 */
    function proofCount() external view returns (uint256) {
        return proofs.length;
    }

    /**
     * 按序号查询存证
     * @param index 存证序号（从 0 开始）
     */
    function getProof(uint256 index) external view returns (Proof memory) {
        require(index < proofs.length, "out of range");
        return proofs[index];
    }

    /**
     * 按 hash 查询存证序号
     * @return 序号+1，0 表示该 hash 未存证
     */
    function getProofIndexByHash(bytes32 hash) external view returns (uint256) {
        return hashIndex[hash];
    }
}
