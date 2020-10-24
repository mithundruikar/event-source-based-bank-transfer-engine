package com.ocbc.bank.dto;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.junit4.SpringRunner;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@DataJpaTest
public class TransactionLogRepositoryTest {

    @Autowired
    TransactionLogRepository transactionLogRepository;

    @Test
    public void findByAccountId() {
        List<TransactionLog> abcEvents = transactionLogRepository.getOrderedLogs("ABC");
        assertEquals(5, abcEvents.size());

        List<TransactionLog> xyzEvents = transactionLogRepository.getOrderedLogs("XYZ");
        assertEquals(2, xyzEvents.size());

        List<TransactionLog> defEvents = transactionLogRepository.getOrderedLogs("DEF");
        assertEquals(2, defEvents.size());
    }

    @Test
    public void persistence() {
        TransactionLog transactionLog = new TransactionLog("001", "002", "PENDING", new BigDecimal("100.1"), "D", LocalDateTime.now());
        assertNull(transactionLog.getId());
        transactionLogRepository.save(transactionLog);

        List<TransactionLog> byAccountId = transactionLogRepository.getOrderedLogs("001");
        assertEquals(transactionLog, byAccountId.get(0));
        assertNotNull(byAccountId.get(0).getId());
    }


    @Test(expected = DataIntegrityViolationException.class)
    public void persistenceWithInvalidArguments() {
        TransactionLog transactionLog = new TransactionLog("001", "002", "PENDING", new BigDecimal("100.1"), "I", LocalDateTime.now());
        transactionLogRepository.save(transactionLog);
        List<TransactionLog> byAccountId = transactionLogRepository.getOrderedLogs("001");
        assertEquals(transactionLog, byAccountId.get(0));
    }
}