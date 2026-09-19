package com.spingbootinit.context.UserContext;

// 线程本地存储
public class UserContext {

    private static final ThreadLocal<Long> userIdHolder = new ThreadLocal<>();
    private static final ThreadLocal<String> usernameHolder = new ThreadLocal<>();

    public static void setCurrenUserId(Long userId){
        userIdHolder.set(userId);
    }

    public static Long getCurrentUserId(){
        return userIdHolder.get();
    }

    public static String getCurrentRole(){
        return usernameHolder.get();
    }
    public static void setCurrentRole(String role) {
        usernameHolder.set(role);
    }

    public static void setCurrentUsername(String username) {
        usernameHolder.set(username);
    }

    public static String getCurrentUsername() {
        return usernameHolder.get();
    }

    // 清理所有
    public static void clear(){
        userIdHolder.remove();
        usernameHolder.remove();
    }


}
