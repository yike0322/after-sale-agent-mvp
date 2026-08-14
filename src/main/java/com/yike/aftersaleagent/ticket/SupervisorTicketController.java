package com.yike.aftersaleagent.ticket;

import com.yike.aftersaleagent.common.api.ApiResponse;
import com.yike.aftersaleagent.common.trace.RequestIdFilter;
import com.yike.aftersaleagent.identity.CurrentDemoUser;
import com.yike.aftersaleagent.identity.DemoUserContext;
import com.yike.aftersaleagent.identity.SupervisorAuthorization;
import com.yike.aftersaleagent.ticket.api.TicketListResponse;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/supervisor/tickets")
public class SupervisorTicketController {
    private final DemoUserContext demoUserContext;
    private final SupervisorAuthorization supervisorAuthorization;
    private final TicketQueryService ticketQueryService;

    public SupervisorTicketController(
            DemoUserContext demoUserContext,
            SupervisorAuthorization supervisorAuthorization,
            TicketQueryService ticketQueryService) {
        this.demoUserContext = demoUserContext;
        this.supervisorAuthorization = supervisorAuthorization;
        this.ticketQueryService = ticketQueryService;
    }

    @GetMapping
    public ApiResponse<TicketListResponse> list(@RequestParam(required = false) String status) {
        CurrentDemoUser user = demoUserContext.requireCurrentUser();
        supervisorAuthorization.requireSupervisor(user);
        return ApiResponse.success(ticketQueryService.listAllTickets(status),
                MDC.get(RequestIdFilter.REQUEST_ID_MDC_KEY));
    }
}
