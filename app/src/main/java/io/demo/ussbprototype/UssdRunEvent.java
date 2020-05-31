package io.demo.ussbprototype;

public class UssdRunEvent {
    public final String className;
    public final String packageName;
    public final long time;
    public final String message;

    public UssdRunEvent(String className, String packageName, long time, String message ) {
       this.className = className;
       this.packageName = packageName;
       this.time = time;
       this.message = message;
    }
}
