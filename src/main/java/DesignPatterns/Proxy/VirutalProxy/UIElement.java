package DesignPatterns.Proxy.VirutalProxy;

public class UIElement {
    String msg = "";

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
        System.out.println("Message set to : " + msg);
    }
}
