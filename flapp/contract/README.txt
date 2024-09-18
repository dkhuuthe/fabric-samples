Create channel (mychannel) for deploying the chaincode:
./network.sh up createChannel -c mychannel -ca

Deploy chaincode to mychannel, with the name of chaincode is FLchaincode:
./network.sh deployCC -ccn FLchaincode -ccp /home/kdam/temp/CODE/chaincode -ccl java -ccep "OR('Org1MSP.peer','Org2MSP.peer')"

./network.sh down