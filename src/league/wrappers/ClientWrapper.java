package league.wrappers;

/**
 * Copyright (c) 2009
 * @author Matus Goljer
 */
public interface ClientWrapper {

    void sendPublicMsg(String channel, String text);

    void sendPrivateMsg(String target, String text);

    void sendNoticeMsg(String target, String text);

    void sendActionMsg(String target, String text);

    void sendKickMsg(String channel, String target, String text);

    void sendModeMsg(String channel, String modeString);

    void sendBroadcastPublicMsg(String text);

    void sendDefaultPublicMsg(String text);

    void sendTopicMsg(String channel, String text);

    void sendInviteMsg(String channel, String userNick);

    String getAuth(String nick);

    String getNick(String auth);

    String getMask(String nick);

    String getNickFromMask(String mask);

    String getType();
    
    void requestAuthAndInvite(String userNick);
}
