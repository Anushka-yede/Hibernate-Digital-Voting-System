require("@nomicfoundation/hardhat-toolbox");
require("dotenv").config();

module.exports = {
  solidity: "0.8.24",
  networks: {
    ganache: {
      url: process.env.GANACHE_RPC_URL || "http://127.0.0.1:7545",
      accounts: process.env.GANACHE_PRIVATE_KEY ? [process.env.GANACHE_PRIVATE_KEY] : []
    }
  }
};
