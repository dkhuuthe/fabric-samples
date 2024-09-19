
import torch
import pickle
import base64
import sys
import hashlib
import time
import random


from py4j.java_gateway import JavaGateway

from torch.utils.data import Dataset
from torchvision import datasets
from torchvision.transforms import ToTensor
from torch.utils.data import DataLoader


device = (
    "cuda"
    if torch.cuda.is_available()
    else "mps"
    if torch.backends.mps.is_available()
    else "cpu"
)
print(f"Using {device} device")


class NeuralNetwork(torch.nn.Module):
    def __init__(self):
        super().__init__()
        self.flatten = torch.nn.Flatten()
        self.linear_relu_stack = torch.nn.Sequential(
            torch.nn.Linear(28*28, 512),
            torch.nn.ReLU(),
            torch.nn.Linear(512, 512),
            torch.nn.ReLU(),
            torch.nn.Linear(512, 10),
        )

    def forward(self, x):
        x = self.flatten(x)
        logits = self.linear_relu_stack(x)
        return logits

class FLTrainer:
    FAILED = "Failed"
    SUCCESS = "OK"
    mapp = JavaGateway().entry_point
    MLmodel = NeuralNetwork().to(device)
    
    def __init__(self):
        super(FLTrainer,self).__init__()
        learning_rate = 1e-3
        self.opt = torch.optim.SGD(FLTrainer.MLmodel.parameters(), lr=learning_rate)
        self.criteria = torch.nn.CrossEntropyLoss()
        self.cid = FLTrainer.genID()
        self.mid = FLTrainer.hashModel(FLTrainer.MLmodel)
        self.blocknumber,self.sessionID = None, None #self.register()
        
    def register(self):
        para = torch.nn.utils.parameters_to_vector(FLTrainer.MLmodel.parameters())
        para_serialized = FLTrainer.para_serialize(para)
        ret = FLTrainer.mapp.register(self.cid, self.mid, para_serialized)
        if ret is None:
            print("register error!")
            return None, None
        return ret["block"],ret["message"]

    
    def train(self,dataloader, model, loss_fn, optimizer):
        size = len(dataloader.dataset)
        batch_size = dataloader.batch_size
        # Set the model to training mode - important for batch normalization and dropout layers
        # Unnecessary in this situation but added for best practices
        model.train()
        for batch, (X, y) in enumerate(dataloader):
            # Compute prediction and loss
            pred = model(X)
            loss = loss_fn(pred, y)
    
            # Backpropagation
            loss.backward()
            optimizer.step()
            optimizer.zero_grad()
    
            if batch % 100 == 0:
                loss, current = loss.item(), batch * batch_size + len(X)
                print(f"loss: {loss:>7f}  [{current:>5d}/{size:>5d}]")
        return FLTrainer.SUCCESS

    def test(self, dataloader, model, loss_fn):
        # Set the model to evaluation mode - important for batch normalization and dropout layers
        # Unnecessary in this situation but added for best practices
        model.eval()
        size = len(dataloader.dataset)
        num_batches = len(dataloader)
        test_loss, correct = 0, 0
    
        # Evaluating the model with torch.no_grad() ensures that no gradients are computed during test mode
        # also serves to reduce unnecessary gradient computations and memory usage for tensors with requires_grad=True
        with torch.no_grad():
            for X, y in dataloader:
                pred = model(X)
                test_loss += loss_fn(pred, y).item()
                correct += (pred.argmax(1) == y).type(torch.float).sum().item()
    
        test_loss /= num_batches
        correct /= size
        result = f"Test Error: \n Accuracy: {(100*correct):>0.1f}%, Avg loss: {test_loss:>8f} \n"
        print(result)

        ret = FLTrainer.mapp.set_result(self.cid, self.mid,result )

    def send_update(self):
        para = torch.nn.utils.parameters_to_vector(FLTrainer.MLmodel.parameters())
        para_serialized = FLTrainer.para_serialize(para)
        
        ret = FLTrainer.mapp.send(self.sessionID,self.cid, self.mid, para_serialized)
        if ((ret is None) | (len(ret)<=0)):
            return FLTrainer.FAILED

        print(f"nClientUpdate: {ret}")
        return ret

    def do_agg(self):
        print("****do aggregation")
        ret = FLTrainer.mapp.get(self.sessionID)
        #print(ret==None)
        agg_para = None
        count = 0
        while ((ret != None) & (len(ret) >0) ):
            para = FLTrainer.para_deserialize(ret)
            #print(para)
            if agg_para is None:
                agg_para = para
            else:
                agg_para = torch.add(agg_para, para)
            ret = FLTrainer.mapp.get(self.sessionID)
            count+=1
        if count > 0:
            agg_para = torch.div(agg_para, count)
            ret = FLTrainer.mapp.sendAgg(self.sessionID,self.mid, FLTrainer.para_serialize(agg_para))
        else:
            ret = None
        if ((ret is None)):
            return FLTrainer.FAILED
        elif (len(ret)<=0):
            return FLTrainer.FAILED
        return FLTrainer.SUCCESS

    def receive_update(self):
        ret = FLTrainer.mapp.getAgg(self.sessionID,self.cid, self.mid)
        if ((ret is None)):
            return FLTrainer.FAILED
        elif (len(ret)<=0):
            return FLTrainer.FAILED
        para = FLTrainer.para_deserialize(ret)
        torch.nn.utils.vector_to_parameters(para,FLTrainer.MLmodel.parameters())
        return FLTrainer.SUCCESS

    def wait_for_agg(self,ucode):
        ret = FLTrainer.mapp.wait_for_agg(self.sessionID,ucode)
        print(ret)
        return ucode


    def run(self,trainData, testData, nrounds=1, nepochs=1):
        #nrounds = 1
        #nepochs = 1
        
        for r in range(1,nrounds+1):
            print(f"round {r}/{nrounds}")
            self.blocknumber,self.sessionID = self.register()
            time.sleep(5)
            print(f"**sessionID {self.sessionID} blocknumber {self.blocknumber}")
            print(f"**modelID {self.mid} ID {self.cid}")
            for epoch in range(1, nepochs+1):
                print(f"***epoch {epoch}/{nepochs}")
                self.train(trainData, FLTrainer.MLmodel, self.criteria, self.opt)
            ucode = self.send_update()
            print(f"send update get {ucode}")
            if self.wait_for_agg(ucode)!="-1":
                self.do_agg()
            
            self.receive_update()
            self.test(testData, FLTrainer.MLmodel, self.criteria)
            
            self.wait_for_agg("end_round")
            

    def __str__(self):
        return self.cid+" "+self.mid 

    @staticmethod
    def para_deserialize(value):
        v_str = value.encode("UTF_8")
        v_str = base64.b64decode(v_str)
        v = pickle.loads(v_str) #json.loads(v_str)
        return v
    
    @staticmethod
    def para_serialize(para):
        v = pickle.dumps(para) 
        v = base64.b64encode(v)
        v = v.decode("UTF_8")
        v_str = v
        #print("size", sys.getsizeof(v))
        return v_str

    @staticmethod
    def hashModel(model):
        data = model.__str__()
        ret = hashlib.md5(data.encode("UTF_8")).hexdigest()
        return ret
    
    @staticmethod
    def genID():
        t = time.time() * 1000
        r = random.random()
        data = str(t)+' '+str(r)
        ret = hashlib.md5(data.encode("UTF_8")).hexdigest()
        return ret
            
if __name__ == "__main__":
    
    training_data = datasets.FashionMNIST(
        root="data",
        train=True,
        download=True,
        transform=ToTensor()
    )
    
    test_data = datasets.FashionMNIST(
        root="data",
        train=False,
        download=True,
        transform=ToTensor()
    )
    
    train_dataloader = DataLoader(training_data, batch_size=64, shuffle=True)
    test_dataloader = DataLoader(test_data, batch_size=64, shuffle=True)

    FLTrainer.mapp.reset()
    
    myapp = FLTrainer()
    
    print(myapp.sessionID, myapp.cid, myapp.mid)
    myapp.run(train_dataloader, test_dataloader,5,1)
