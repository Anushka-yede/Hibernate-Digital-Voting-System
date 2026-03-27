package com.securevote.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.gas.DefaultGasProvider;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;

@Service
public class BlockchainService {

    private static final Logger log = LoggerFactory.getLogger(BlockchainService.class);

    private final Web3j web3j;

    @Value("${blockchain.private-key:}")
    private String privateKey;

    @Value("${blockchain.contract-address:}")
    private String contractAddress;

    public BlockchainService(Web3j web3j) {
        this.web3j = web3j;
    }

    public String generateVoteHash(Long userId, Long candidateId, Instant timestamp) {
        try {
            String payload = userId + ":" + candidateId + ":" + timestamp.toString();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to generate vote hash", ex);
        }
    }

    public String storeVoteHashOnChain(String voteHash) {
        try {
            if (privateKey == null || privateKey.isBlank() || contractAddress == null || contractAddress.isBlank()) {
                log.warn("Blockchain private key or contract address missing. Returning local-only marker.");
                return "LOCAL_ONLY_" + System.currentTimeMillis();
            }

            Credentials credentials = Credentials.create(privateKey);
            RawTransactionManager txManager = new RawTransactionManager(web3j, credentials);
            Function function = new Function(
                "storeVoteHash",
                List.of(new Bytes32(hexToBytes32(voteHash))),
                Collections.emptyList()
            );
            String data = FunctionEncoder.encode(function);

            EthSendTransaction tx = txManager.sendTransaction(
                    DefaultGasProvider.GAS_PRICE,
                    DefaultGasProvider.GAS_LIMIT,
                    contractAddress,
                data,
                    BigInteger.ZERO
            );

            if (tx.hasError()) {
                log.error("Blockchain error: {}", tx.getError().getMessage());
                return "CHAIN_ERROR_" + tx.getError().getCode();
            }

            return tx.getTransactionHash();
        } catch (Exception ex) {
            log.error("Failed to write hash on chain", ex);
            return "CHAIN_EXCEPTION";
        }
    }

    public boolean verifyAnchored(String txHash) {
        if (txHash == null || txHash.isBlank()) {
            return false;
        }
        if (txHash.startsWith("LOCAL_ONLY") || txHash.startsWith("CHAIN_ERROR") || txHash.equals("CHAIN_EXCEPTION")) {
            return false;
        }

        try {
            return web3j.ethGetTransactionByHash(txHash).send().getTransaction().isPresent();
        } catch (Exception ex) {
            log.warn("Failed to verify transaction {}", txHash, ex);
            return false;
        }
    }

    public boolean verifyVoteHashOnChain(String voteHash) {
        if (voteHash == null || voteHash.isBlank() || contractAddress == null || contractAddress.isBlank()) {
            return false;
        }

        try {
            Function function = new Function(
                    "verifyVoteHash",
                    List.of(new Bytes32(hexToBytes32(voteHash))),
                    List.of(new TypeReference<Bool>() {
                    })
            );

            String encoded = FunctionEncoder.encode(function);
            Transaction call = Transaction.createEthCallTransaction(null, contractAddress, encoded);
            String value = web3j.ethCall(call, DefaultBlockParameterName.LATEST).send().getValue();
            if (value == null || value.equals("0x")) {
                return false;
            }

            List<org.web3j.abi.datatypes.Type> decoded = FunctionReturnDecoder.decode(value, function.getOutputParameters());
            return !decoded.isEmpty() && (Boolean) decoded.get(0).getValue();
        } catch (Exception ex) {
            log.warn("Failed to verify vote hash on chain", ex);
            return false;
        }
    }

    private byte[] hexToBytes32(String voteHash) {
        String normalized = voteHash.startsWith("0x") ? voteHash.substring(2) : voteHash;
        if (normalized.length() != 64) {
            throw new IllegalArgumentException("Vote hash must be 32 bytes (64 hex chars)");
        }
        return HexFormat.of().parseHex(normalized);
    }

    public boolean verifyAnchoredWithHash(String txHash, String voteHash) {
        if (txHash == null || txHash.isBlank()) {
            return false;
        }
        if (txHash.startsWith("LOCAL_ONLY") || txHash.startsWith("CHAIN_ERROR") || txHash.equals("CHAIN_EXCEPTION")) {
            return false;
        }

        return verifyAnchored(txHash) && verifyVoteHashOnChain(voteHash);
    }
}
