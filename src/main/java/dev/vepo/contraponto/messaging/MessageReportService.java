package dev.vepo.contraponto.messaging;

import dev.vepo.contraponto.user.User;
import dev.vepo.contraponto.user.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class MessageReportService {

    private final MessageReportRepository reportRepository;
    private final UserRepository userRepository;

    @Inject
    public MessageReportService(MessageReportRepository reportRepository, UserRepository userRepository) {
        this.reportRepository = reportRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public MessageReport dismiss(long reportId, long adminUserId) {
        MessageReport report = requirePendingReport(reportId);
        User reviewer = userRepository.findById(adminUserId).orElseThrow(NotFoundException::new);
        report.dismiss(reviewer);
        return reportRepository.save(report);
    }

    public MessageReportDetail loadDetail(long reportId) {
        MessageReport report = reportRepository.findById(reportId).orElseThrow(NotFoundException::new);
        var messages = reportRepository.findThreadMessagesForReport(report.getThread().getId());
        return new MessageReportDetail(report, messages);
    }

    @Transactional
    public MessageReport markReviewed(long reportId, long adminUserId) {
        MessageReport report = requirePendingReport(reportId);
        User reviewer = userRepository.findById(adminUserId).orElseThrow(NotFoundException::new);
        report.markReviewed(reviewer);
        return reportRepository.save(report);
    }

    private MessageReport requirePendingReport(long reportId) {
        MessageReport report = reportRepository.findById(reportId).orElseThrow(NotFoundException::new);
        if (report.getStatus() != MessageReportStatus.PENDING) {
            throw new NotFoundException("Report not found.");
        }
        return report;
    }
}
