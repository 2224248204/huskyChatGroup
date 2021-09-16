package cn.luridang.huskychat.socket;

import com.alibaba.fastjson.JSON;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.*;

/**
 * socket
 */
@ServerEndpoint("/huskychat/{groupId}")
public class WebSocket {
    private Session session;

    // 存放所有聊天室的集合
    private static Map<String, Collection<WebSocket>> servers = new Hashtable<>();

    /**
     * 响应内容的集合
     * type 响应的类型(新消息，连接人数)
     * content 响应的内容
     */
    Map<String, Object> responseMessage = new Hashtable<>();

    /**
     * 创建一个连接会话
     * @param session
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("groupId") String groupId){
        this.session = session;

        // 聊天室里的群聊对象
        Collection<WebSocket> list;

        // 判断该集合下是否已经有了该聊天室
        if(!servers.containsKey(groupId)){
            list = Collections.synchronizedCollection(new ArrayList<WebSocket>());
            list.add(this);
            servers.put(groupId, list);
        }else{
            servers.get(groupId).add(this);
        }

        responseMessage.put("type", "newConn");
        message(servers.get(groupId).size(), groupId);
        System.out.println("有新的连接加入" + groupId + "，当前人数：" + servers.get(groupId).size());
    }

    /**
     * 关闭当前连接
     */
    @OnClose
    public void onClose(@PathParam("groupId") String groupId){
        servers.remove(this);
        Collection<WebSocket> list = servers.get(groupId);
        if(list.size() <= 0){
            servers.remove(groupId);
        }else{
            servers.get(groupId).remove(this);
        }
        responseMessage.put("type", "noneConn");
        message(servers.get(groupId).size(), groupId);
        System.out.println("有连接关闭" + groupId + "，当前人数：" + servers.get(groupId).size());
    }

    /**
     * 连接异常
     */
    @OnError
    public void onError(Session session, Throwable error, @PathParam("groupId") String groupId){
        System.out.println("连接出现了异常！");

        responseMessage.put("type", "error");
        message("连接出现了异常", groupId);
        error.printStackTrace();
    }

    /**
     * 监听消息内容
     * @param message
     */
    @OnMessage
    public void onMessage(String message, Session session, @PathParam("groupId") String groupId){
        System.out.println("消息内容：" + message);
        responseMessage.put("type", "newMess");
        message(message, groupId);
    }

    /**
     * 发送消息
     * @param mess
     */
    public void sendMessage(String mess){
        try {
            this.session.getBasicRemote().sendText(mess);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 响应消息返回给用户
     * @param message
     */
    public void message(Object message, String groupId){
        String type = (String) responseMessage.get("type");
        if(type.equals("num")){
            responseMessage.put("data", String.valueOf(message));
        }else if(type.equals("newConn")){
            responseMessage.put("data", String.valueOf(message));
        }else if(type.equals("noneConn")){
            responseMessage.put("data", String.valueOf(message));
        }else if(type.equals("error")){
            responseMessage.put("data", String.valueOf(message));
        }else if(type.equals("newMess")){
            responseMessage.put("data", message);
        }

        // 发送消息给每一个对象
        /*for (WebSocket socket:
                servers) {
            socket.sendMessage(JSON.toJSONString(responseMessage));
        }*/

        Collection<WebSocket> list = servers.get(groupId);

        for (WebSocket socket : list) {
            socket.sendMessage(JSON.toJSONString(responseMessage));
        }
    }
}
