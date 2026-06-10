package DesignPatterns.ChainOfResponsiblity;


public class ObjectProcessorAToLowerCase extends ProcessingObject<String> {
    @Override
    protected String handleWork(String input) {
        var res = input.toLowerCase();
        System.out.println("In processor : " + res);
        return res;
    }
}
