package com.yike.aftersaleagent.ticket;

import com.yike.aftersaleagent.common.api.ApiResponse;
import com.yike.aftersaleagent.common.trace.RequestIdFilter;
import com.yike.aftersaleagent.identity.CurrentDemoUser;
import com.yike.aftersaleagent.identity.DemoUserContext;
import com.yike.aftersaleagent.ticket.api.TicketDetailResponse;
import com.yike.aftersaleagent.ticket.api.TicketListResponse;
import com.yike.aftersaleagent.ticket.api.TicketTraceResponse;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {
    private final DemoUserContext demoUserContext;
    private final TicketQueryService ticketQueryService;

    public TicketController(
            DemoUserContext demoUserContext,
            TicketQueryService ticketQueryService) {
        this.demoUserContext = demoUserContext;
        this.ticketQueryService = ticketQueryService;
    }

    @GetMapping("/mine")
    public ApiResponse<TicketListResponse> mine(@RequestParam(required = false) String status) {
        CurrentDemoUser user = demoUserContext.requireCurrentUser();
        return ApiResponse.success(ticketQueryService.listOwnedTickets(user.id(), status), requestId());
    }

    @GetMapping("/{ticketId}")
    public ApiResponse<TicketDetailResponse> detail(@PathVariable long ticketId) {
        CurrentDemoUser user = demoUserContext.requireCurrentUser();
        return ApiResponse.success(ticketQueryService.getOwnedDetail(user.id(), ticketId), requestId());
    }

    @GetMapping("/{ticketId}/trace")
    public ApiResponse<TicketTraceResponse> trace(@PathVariable long ticketId) {
        CurrentDemoUser user = demoUserContext.requireCurrentUser();
        return ApiResponse.success(ticketQueryService.getOwnedTrace(user.id(), ticketId), requestId());
    }

    private String requestId() {
        return MDC.get(RequestIdFilter.REQUEST_ID_MDC_KEY);
    }
}
