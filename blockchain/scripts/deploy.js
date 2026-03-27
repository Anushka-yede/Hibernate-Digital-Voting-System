async function main() {
  const VoteHashStore = await ethers.getContractFactory("VoteHashStore");
  const contract = await VoteHashStore.deploy();
  await contract.waitForDeployment();

  console.log("VoteHashStore deployed to:", await contract.getAddress());
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
