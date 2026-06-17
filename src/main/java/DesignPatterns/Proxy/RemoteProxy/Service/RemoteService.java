package DesignPatterns.Proxy.RemoteProxy.Service;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteService extends Remote {
    void callRemoteService() throws RemoteException;

    String getRandomString() throws RemoteException;
}
