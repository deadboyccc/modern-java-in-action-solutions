package DesignPatterns.Proxy.RemoteProxy.Service;

import java.io.Serial;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.UUID;

public class RemoteServiceImpl
        extends UnicastRemoteObject // to give it the remote abilities through delegation to the super class
        implements RemoteService  // the client interface the client expects
{
    @Serial
    private static final long serialVersionUID = 1L;

    protected RemoteServiceImpl() throws RemoteException {
    }

    @Override
    public void callRemoteService() throws RemoteException {
        System.out.println("An RPC call ...");


    }

    @Override
    public String getRandomString() throws RemoteException {
        return UUID.randomUUID().toString();
    }
}
