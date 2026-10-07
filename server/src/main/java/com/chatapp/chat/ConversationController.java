package com.chatapp.chat;

import com.chatapp.security.AuthUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;
    private final MessageService messageService;

    public record OpenDirectRequest(@NotNull Long userId) {
    }

    public record CreateGroupRequest(
            @NotBlank @Size(max = 100) String name,
            @NotEmpty @Size(max = 100) List<@NotNull Long> memberIds) {
    }

    /** Sidebar: DM + group của user, mới nhất trước. */
    @GetMapping
    public List<ConversationDto> list(@AuthenticationPrincipal AuthUser me) {
        return conversationService.sidebar(me.id());
    }

    @GetMapping("/{id}")
    public ConversationDto get(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return conversationService.get(me.id(), id);
    }

    @PostMapping("/direct")
    public ConversationDto openDirect(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody OpenDirectRequest req) {
        return conversationService.openDirect(me.id(), req.userId());
    }

    @PostMapping("/group")
    @ResponseStatus(HttpStatus.CREATED)
    public ConversationDto createGroup(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody CreateGroupRequest req) {
        return conversationService.createGroup(me.id(), req.name(), req.memberIds());
    }

    @GetMapping("/{id}/messages")
    public List<MessageDto> history(
            @AuthenticationPrincipal AuthUser me,
            @PathVariable Long id,
            @RequestParam(required = false) Long before,
            @RequestParam(required = false) Integer limit) {
        return messageService.history(me.id(), id, before, limit);
    }

    /** Gửi tin qua REST — tương đương STOMP /app/conversations/{id}/send, tiện cho test hoặc fallback. */
    @PostMapping("/{id}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public MessageDto send(
            @AuthenticationPrincipal AuthUser me,
            @PathVariable Long id,
            @Valid @RequestBody SendMessageRequest req) {
        return messageService.send(me.id(), id, req);
    }
}
