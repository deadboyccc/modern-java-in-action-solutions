package DesignPatterns.Proxy.RemoteProxy.Client;

import DesignPatterns.Proxy.RemoteProxy.Service.RemoteService;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

public class RemoteServiceClient {
    static void main() throws MalformedURLException, NotBoundException, RemoteException {
        RemoteService remoteService = (RemoteService) Naming.lookup("rmi://127.0.0.1/RemoteService");
        System.out.println("Getting a random String from the remote service" + remoteService.getRandomString());
        System.out.println("Calling the remote service..");
        remoteService.callRemoteService();


    }

}
