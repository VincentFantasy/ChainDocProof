// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

/**
 * DocProof：文件存证合约
 * 把文件 SHA-256 与时间戳上链，之后可验证文件是否被篡改
 * （DocToken 在购买次数时由用户 burn 销毁，本合约不再消耗代币）
 * 部署时指定管理端地址（owner），只有 owner 能调用 storeProof 存证；
 * 查询函数（proofCount / getProof / getProofIndexByHash）保持公开，
 * 只读调用走 eth_call，msg.sender 可随意伪造，加 onlyOwner 没有安全意义
 */
contract DocProof {
    struct Proof {
        uint256 index; // 存证序号（从 0 开始）
        bytes32 hash; // 文件 SHA-256
        uint256 timestamp; // 文件上传时间戳（秒）
        address user; // 存证用户钱包地址（后端传入）
        uint256 blockTime; // 上链区块时间戳（秒）
    }

    /// 存证列表
    Proof[] public proofs;
    /// hash -> 存证序号+1（0 表示不存在，避免与序号 0 冲突）
    mapping(bytes32 => uint256) public hashIndex;
    /// 管理端地址（后端存证钱包，只有它能存证）
    address public owner;

    event ProofStored(uint256 indexed index, bytes32 hash, uint256 timestamp, address indexed user);

    modifier onlyOwner() {
        require(msg.sender == owner, "only owner");
        _;
    }

    /**
     * 构造函数：显式指定管理端地址（只有它能调用 storeProof 存证），
     * 不要求部署者与 adminAddr 是同一地址
     *
     * @param adminAddr 管理端地址（后端存证钱包地址）
     */
    constructor(address adminAddr) {
        require(adminAddr != address(0), "invalid admin");
        owner = adminAddr;
    }

    /**
     * 存证：把 hash + 时间戳 + 用户地址上链（只有管理端可调用）
     * （不消耗 DocToken，次数由后端在调用前扣减）
     *
     * @param hash      文件 SHA-256
     * @param timestamp 文件上传时间戳（秒）
     * @param user      存证用户钱包地址（msg.sender 恒为管理端钱包，故由后端显式传入）
     */
    function storeProof(bytes32 hash, uint256 timestamp, address user) external onlyOwner {
        require(hash != bytes32(0) && user != address(0), "invalid params");
        uint256 index = proofs.length;
        proofs.push(Proof({index: index, hash: hash, timestamp: timestamp, user: user, blockTime: block.timestamp}));
        if (hashIndex[hash] == 0) {
            hashIndex[hash] = index + 1;
        }
        emit ProofStored(index, hash, timestamp, user);
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
