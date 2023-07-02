package com.tms.lib.model;

public enum DefaultIsoResponseCodes {

    Approved("00"),
    ReferToCardIssuer("01"),
    ReferToCardIssuerSpecialCondition("02"),
    InvalidMerchant("03"),
    PickUpCard("04"),
    DoNotHonor("05"),
    Error("06"),
    PickUpCardSpecialCondition("07"),
    HonorWithIdentification("08"),
    RequestInProgress("09"),
    ApprovedPartial("10"),
    ApprovedVip("11"),
    InvalidTransaction("12"),
    InvalidAmount("13"),
    InvalidCardNumber("14"),
    NoSuchIssuer("15"),
    ApprovedUpdateTrack3("16"),
    CustomerCancellation("17"),
    CustomerDispute("18"),
    ReEnterTransaction("19"),
    InvalidResponse("20"),
    NoActionTaken("21"),
    SuspectedMalfunction("22"),
    UnacceptableTransactionFee("23"),
    FileUpdateNotSupported("24"),
    UnableToLocateRecord("25"),
    DuplicateRecord("26"),
    FileUpdateFieldEditError("27"),
    FileUpdateFileLocked("28"),
    FileUpdateFailed("29"),
    FormatError("30"),
    BankNotSupported("31"),
    CompletedPartially("32"),
    ExpiredCardPickUp("33"),
    SuspectedFraudPickUp("34"),
    ContactAcquirerPickUp("35"),
    RestrictedCardPickUp("36"),
    CallAcquirerSecurityPickUp("37"),
    PinTriesExceededPickUp("38"),
    NoCreditAccount("39"),
    FunctionNotSupported("40"),
    LostCardPickUp("41"),
    NoUniversalAccount("42"),
    StolenCardPickUp("43"),
    NoInvestmentAccount("44"),
    AccountClosed("45"),
    IdentificationRequired("46"),
    IdentificationCrossCheckRequired("47"),
    InsufficientFunds("51"),
    NoCheckAccount("52"),
    NoSavingsAccount("53"),
    ExpiredCard("54"),
    IncorrectPin("55"),
    NoCardRecord("56"),
    TransactionNotPermittedToCardHolder("57"),
    TransactionNotPermittedOnTerminal("58"),
    SuspectedFraud("59"),
    ContactAcquirer("60"),
    ExceedsWithdrawalLimit("61"),
    RestrictedCard("62"),
    SecurityViolation("63"),
    OriginalAmountIncorrect("64"),
    ExceedsWithdrawalFrequency("65"),
    CallAcquirerSecurity("66"),
    HardCapture("67"),
    ResponseReceivedTooLate("68"),
    AdviceReceivedTooLate("69"),
    PinTriesExceeded("75"),
    NoMatchForRetrievalReferenceNumber("76"),
    BlockedFirstUse("78"),
    IssuerOrSwitchInOperative("91"),
    RoutingError("92"),
    TransactionCannotBeCompletedViolationOfLaw("93"),
    DuplicateTransaction("94"),
    ReconcileError("95"),
    SystemMalFunction("96"),
    ExceedsCashLimit("98");


    private DefaultIsoResponseCodes(final String text) {
        this.text = text;
    }

    private final String text;

    @Override
    public String toString() {
        return text;
    }

    public String toWordyString() {
        return super.toString();
    }

    public static DefaultIsoResponseCodes fromString(String text) {
        if (text != null) {
            for (DefaultIsoResponseCodes b : DefaultIsoResponseCodes.values()) {
                if (text.equalsIgnoreCase(b.text)) {
                    return b;
                }
            }
        }
        throw new IllegalArgumentException(String.format("Unknown iso response code %s", text));
    }

    public String getText() {
        return fromString(text).toWordyString();
    }
}
