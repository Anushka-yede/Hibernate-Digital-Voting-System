// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

contract VoteHashStore {
    mapping(bytes32 => bool) public voteHashExists;
    bytes32[] private allHashes;

    event VoteHashStored(bytes32 indexed voteHash, address indexed submittedBy, uint256 timestamp);

    function storeVoteHash(bytes32 voteHash) external {
        require(voteHash != bytes32(0), "Invalid hash");
        require(!voteHashExists[voteHash], "Hash already exists");

        voteHashExists[voteHash] = true;
        allHashes.push(voteHash);

        emit VoteHashStored(voteHash, msg.sender, block.timestamp);
    }

    function verifyVoteHash(bytes32 voteHash) external view returns (bool) {
        return voteHashExists[voteHash];
    }

    function totalHashes() external view returns (uint256) {
        return allHashes.length;
    }
}
