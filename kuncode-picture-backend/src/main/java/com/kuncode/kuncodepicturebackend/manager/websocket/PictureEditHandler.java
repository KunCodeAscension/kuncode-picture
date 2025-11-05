package com.kuncode.kuncodepicturebackend.manager.websocket;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.kuncode.kuncodepicturebackend.manager.websocket.disruptor.PictureEditEventProducer;
import com.kuncode.kuncodepicturebackend.manager.websocket.model.PictureEditActionEnum;
import com.kuncode.kuncodepicturebackend.manager.websocket.model.PictureEditMessageTypeEnum;
import com.kuncode.kuncodepicturebackend.manager.websocket.model.PictureEditRequestMessage;
import com.kuncode.kuncodepicturebackend.manager.websocket.model.PictureEditResponseMessage;
import com.kuncode.kuncodepicturebackend.model.entity.User;
import com.kuncode.kuncodepicturebackend.model.vo.user.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
@RequiredArgsConstructor
public class PictureEditHandler extends TextWebSocketHandler {

    private final Map<Long, Long> pictureEditUsers = new ConcurrentHashMap<>();

    private final Map<Long, Set<WebSocketSession>> pictureEditSessions = new ConcurrentHashMap<>();

    final PictureEditEventProducer pictureEditEventProducer;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        super.afterConnectionEstablished(session);
        User user =  (User) session.getAttributes().get("user");
        Long pictureId =  (Long) session.getAttributes().get("pictureId");
        pictureEditSessions.putIfAbsent(pictureId,ConcurrentHashMap.newKeySet());
        pictureEditSessions.get(pictureId).add(session);
        PictureEditResponseMessage message = new PictureEditResponseMessage();
        message.setType(PictureEditMessageTypeEnum.INFO.getValue());
        message.setMessage(String.format("用户 %s 加入了编辑",user.getUserName()));
        message.setUser(UserVO.toUserVO(user));
        broadcastToPictures(pictureId, message);
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        super.handleTextMessage(session, message);
        PictureEditRequestMessage pictureEditRequestMessage = JSONUtil.toBean(message.getPayload(), PictureEditRequestMessage.class);
        String type = pictureEditRequestMessage.getType();
        PictureEditMessageTypeEnum enumType = PictureEditMessageTypeEnum.getEnumByValue(type);
        User user =  (User) session.getAttributes().get("user");
        Long pictureId =  (Long) session.getAttributes().get("pictureId");
        pictureEditEventProducer.publishEvent(pictureEditRequestMessage, session, user, pictureId);
    }

    public void handleEnterEditMessage(PictureEditRequestMessage pictureEditRequestMessage, User user, Long pictureId, WebSocketSession session) throws IOException {
        // 进入编辑;
        System.out.println(user.getUserName());
        if (!pictureEditUsers.containsKey(pictureId)) {
            pictureEditUsers.put(pictureId, user.getId());
            PictureEditResponseMessage responseMessage = new PictureEditResponseMessage();
            responseMessage.setType(PictureEditMessageTypeEnum.ENTER_EDIT.getValue());
            responseMessage.setMessage(String.format("用户 %s 开始编辑图片",user.getUserName()));
            responseMessage.setUser(UserVO.toUserVO(user));
            broadcastToPictures(pictureId,responseMessage);
        }
    }

    public void handleEditActionMessage(PictureEditRequestMessage pictureEditRequestMessage, User user, Long pictureId, WebSocketSession session) throws IOException {
        // 处理编辑
        String editAction = pictureEditRequestMessage.getEditAction();
        PictureEditActionEnum editActionEnum = PictureEditActionEnum.getEnumByValue(editAction);
        if(editActionEnum == null){
            log.error("无效的编辑动作");
            return;
        }
        if(pictureEditUsers.containsKey(pictureId) && pictureEditUsers.get(pictureId).equals(user.getId())){
            // 发送具体操作
            PictureEditResponseMessage responseMessage = new PictureEditResponseMessage();
            responseMessage.setType(PictureEditMessageTypeEnum.EDIT_ACTION.getValue());
            responseMessage.setMessage(String.format("用户 %s 执行了 %s 操作",user.getUserName(),pictureEditRequestMessage.getEditAction()));
            responseMessage.setUser(UserVO.toUserVO(user));
            responseMessage.setEditAction(editActionEnum.getValue());
            broadcastToPictures(pictureId,responseMessage,session);
        }
    }

    public void handleExitEditMessage(PictureEditRequestMessage pictureEditRequestMessage, User user, Long pictureId, WebSocketSession session) throws IOException {
        if(pictureEditUsers.containsKey(pictureId) && pictureEditUsers.get(pictureId).equals(user.getId())){
            pictureEditUsers.remove(pictureId);
            PictureEditResponseMessage responseMessage = new PictureEditResponseMessage();
            responseMessage.setType(PictureEditMessageTypeEnum.EXIT_EDIT.getValue());
            responseMessage.setMessage(String.format("用户 %s 退出了编辑",user.getUserName()));
            responseMessage.setUser(UserVO.toUserVO(user));
            broadcastToPictures(pictureId,responseMessage,session);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        super.afterConnectionClosed(session,status);
        User user =  (User) session.getAttributes().get("user");
        Long pictureId =  (Long) session.getAttributes().get("pictureId");
        handleExitEditMessage(null,user,pictureId,session);
        Set<WebSocketSession> webSocketSessions = pictureEditSessions.get(pictureId);
        if(webSocketSessions != null){
            webSocketSessions.remove(session);
            if(webSocketSessions.isEmpty()){
                pictureEditSessions.remove(pictureId);
            }
        }
        PictureEditResponseMessage message = new PictureEditResponseMessage();
        message.setType(PictureEditMessageTypeEnum.INFO.getValue());
        message.setMessage(String.format("用户 %s 离开了编辑",user.getUserName()));
        message.setUser(UserVO.toUserVO(user));
        broadcastToPictures(pictureId,message,session);
    }

    public void broadcastToPictures(Long pictureId, PictureEditResponseMessage message,WebSocketSession excludeSession) throws IOException {
        Set<WebSocketSession> webSocketSessions = pictureEditSessions.get(pictureId);
        if(CollUtil.isNotEmpty(webSocketSessions)){
            ObjectMapper objectMapper = new ObjectMapper();
            SimpleModule module = new SimpleModule();
            module.addSerializer(Long.class, ToStringSerializer.instance);
            module.addSerializer(Long.TYPE, ToStringSerializer.instance);
            objectMapper.registerModule(module);
            // 序列化
            String jsonStr = objectMapper.writeValueAsString(message);
            TextMessage textMessage = new TextMessage(jsonStr);
            for (WebSocketSession webSocketSession : webSocketSessions) {
                if(webSocketSession.isOpen() && !webSocketSession.equals(excludeSession)){
                    webSocketSession.sendMessage(textMessage);
                }
            }
        }
    }

    public void broadcastToPictures(Long pictureId, PictureEditResponseMessage message) throws IOException {
        broadcastToPictures(pictureId, message, null);
    }

}
