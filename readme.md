# Summary
This is a banking cli application. It has 2 types of processes. bank-server and (one or more) bank-client(s). It is based on event sourcing model.  
- bank-server module: maintains durable transaction logs (TRANSACTION_LOG table in H2 currently). These logs hold durable account transactional information. <br>
On restart or any issues, we can recover account state using event sourcing design pattern.  
- bank-client module: it connects to bank-server using exposed rest endpoints. We can have multiple bank-client sessions connecting to the server.

# Build
It is built using maven. There are 2 spring boot applications. One for server and another for client. Used below dependency:
- junit
- guava
- mockito
- h2database
- slf4j
- lombok
- jackson 

# Running Locally
Step 1) (Can be step 2 as well)
Run bank-server.
Main class - `com.ocbc.bank.Main`



Step 2)
Run as many bank-client instance you would like to have parallel login sessions.
Main class - `com.ocbc.bank.client.Main`
Every client will print usage and will accept commands from it's standard input stream.

# Design explanation
- bank-server: <br>
    It is designed with event sourcing in mind. `UserAccount` is the domain aggregate. It accepts one of the command instructions (`IBankingInstruction`) and atomically/durably updates transaction logs. On successful write in the log, it generates events.
    These events are handled by the domains (including self) to update the state. <br>
    Please see one of the command (e.g. `TransferCommand`) to see how client can send commands/requests and these command classes interacts with required `UserAccount` domains.
    There are 3 main types of command which client can indirectly generate:
    - `LogInCommand`
    - `TransferCommand`
    - `TopUpCommand`      
    These commands results in Command instructions which are handled by UserAccount to update transaction log.
    Conditionally, as a result of `ActualCreditedEvent` depending on the owing history, generates `PayoutDebtsInstruction`
    `PayoutDebtsInstruction` can result in a series of payback instructions which are handled asynchronously. 
    
    <br>
    Steps of events on simple Transfer request from A to B. Steps 1 to 8 happens synchronously. <br>
    
        1) Get A from UserAccountRepository. It involves recovering all events from DB for event sourcing to rebuild the state.
        2) Get lock on A
        3) Perform transaction log writing to indicate the transfer
        4) A result of successful log writing is 4 events -> `ActualDebitedEvent`, `ActualCreditedEvent`, `PendingDebitedEvent`, `PendingCreditedEvent`
        Pending events if the balance is not enough for the transfer.
         For Debit events source account is A and counter party is B.
         For Credit events source account is B and counter party is A.
        5) Apply all events generated where source account is A. in this case, `ActualDebitedEvent` and `PendingDebitedEvent`
        6) Release A's lock
        7) Dispatch remaining credit side events (for B in this case) to `CreditSideTransferEventsHandler`.
        8) Reply client with Success/failure response synchronously.
     
    Below happens asynchronously as a continuation of the transfer for credit side generated events: <br>
    
        9)  Get lock on Account. B in this case. (event source from DB if required, just like above for A)
        10) Apply all events generated where primary account is B. in this case, `ActualCreditedEvent` and `PendingCreditedEvent`
        11) For some credits on B, if B ows to say C (and other accounts), then there will be those many `PayoutDebtsInstruction` instruction raised if it is deemed fit one per each owed account. e.g.
            These `PayoutDebtsInstruction` will create durable and atomica Transaction log entry.
            On successful log writing results in `PaybackDebitEvent`, `PaybackCreditEvent`, `ActualDebitedEvent` and `ActualCreditedEvent`
            For Debit events source account is B and counter party is C.
            For Credit events source account is C and counter party is B.
        12) Release B's lock
        13) For remaining credit events group them by source account account id
            In this case `PaybackCreditEvent` and `ActualCreditedEvent` generate as a result of payout to C.
            dispatch these credit message batches per source account for async processing and repeat from step 9.
 
    Same pattern is followed by other commands like `TopCommand`. 
  

# Important Test classes for debugging
- `UserAccountEventSourceTest`
- `TransactionLogRepositoryTest`
- `BankServerInvokerTest`
- `UserAccountTest`

# Things to do
- Idempotency handling - Client ideally should provide a unique id with the request. And server should maintain durable list of ids to handle duplicates.
- Real db - currently using h2
- Security, authentication etc.
- Cucumber acceptance test


