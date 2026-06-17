package DesignPatterns.Proxy.RemoteProxy.Service;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;

public class ServiceEngine {
    static void main() throws RemoteException, MalformedURLException {
        LocateRegistry.createRegistry(1099);
        System.out.println("RMI Registry started inside JVM.");

        RemoteService remoteService = new RemoteServiceImpl();
        Naming.rebind("rmi://127.0.0.1/RemoteService", remoteService);
    }
}
