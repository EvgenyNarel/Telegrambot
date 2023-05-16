package com.example.demobot.service;

import com.example.demobot.config.BotConfig;
import com.example.demobot.model.*;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j;
import org.hibernate.LazyInitializationException;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.File;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.Month;
import java.util.*;

@Component
@Log4j
@RequiredArgsConstructor
public class TelegramBot extends TelegramLongPollingBot {

    @Autowired
    UserRepository userRepository;

    @Autowired
    MassageRepository massageRepository;

    @Autowired
    AppointmentRepository appointmentRepository;

    private final BotConfig config;

    private final String classic = "Классический";
    private final String massageBack = "Спины";
    private final String anti_cellulite = "Антицеллюлитный";

    HashMap<Long, String> selectAddress = new HashMap<>();
    HashMap<Long, Massage> selectMassage = new HashMap<>();
    HashMap<Long, String> selectTime = new HashMap<>();
    HashMap<Long, Boolean> takeOutOrder = new HashMap<>();

    HashMap<Long, Date> selectDate = new HashMap<>();
    HashMap<Long, String> selectNumberPhone = new HashMap<>();

    boolean isTrue;

    private boolean isLeapYear(int year) {

        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0);
    }

    int currentMonth = Calendar.getInstance().get(Calendar.MONTH);
    int currentYear = LocalDate.now().getYear();
    int currentDay = LocalDate.now().getDayOfMonth();

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getBotToken();
    }

    @SneakyThrows
    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
//            int messageId = update.getMessage().getMessageId();
            long chatId = update.getMessage().getChatId();

            log.info(messageText);

            if (messageText.matches("^s*(ул|проспект|пл|пер|бульвар|тупик|аллея|проезд|квартал|городок|микра|тракт|заул).*\\s*[А-Яа-я]+\\s*(д|строение)\\s*\\d*\\s*(кв|квартира)\\s*\\d*\\s*$" +
                    "|^s*(ул|проспект|пл|пер|бульвар|тупик|аллея|проезд|квартал|городок|микра|тракт|заул).*\\s*[А-Яа-я]+\\s*(д|строение)\\s*\\d*$" +
                    "|^s*(ул|проспект|пл|пер|бульвар|тупик|аллея|проезд|квартал|городок|микра|тракт|заул).*\\s*[А-Яа-я]+\\s*(д|строение)\\s*\\d*\\s*(корпус|к)\\s*\\d*\\s*(кв|квартира)\\s*\\d*\\s*$")) {
                isTrue = false;
                selectAddress.put(chatId, messageText);
                validateNumberPhoneFromUser(chatId);
                return;

            } else {

                isTrue = true;

            }
            if (messageText.matches("^((8)(\\s*|\\()((029|033|044|025)\\)?\\s*)(\\d{3}\\s*\\d{2}\\s*\\d{2}))$")) {
                selectNumberPhone.put(chatId, messageText);
                orderDescription(update);
            } else {
                isTrue = false;

            }

            switch (messageText) {
                case "/start" -> {
                    addPhoto(chatId, "src/main/resources/img/candle.jpg");
                    registrationUser(update.getMessage());
                    startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                    buttonsSelectMassage(chatId);
                }
                case "/contacts" -> contactsCommandReceived(chatId);
                case "/admin" -> {
                    if (chatId == 894213461) {
                        dataAdmin(chatId);
                    }
                }
                default -> {

                    if (isTrue) {
                        System.out.println(" ПРОВЕРКА MESSAGE TEXT");

                    } else {
                        sendMessage(chatId, "Что-то пошло не так, пожалуйста повторите ввод данных");

                    }
                }
            }

        } else if (update.hasCallbackQuery()) {
            handleCallBackQuery(update);
            datePickerHandler(update);
            timeSelectionHandler(update);
            appointmentInDB(update);
            dataAdminHandler(update);
        }
    }

    private void startCommandReceived(long chatId, String name) {
        String answer = "Привет, " + name + ", рад познакомиться";
        sendMessage(chatId, answer);
    }

    private void dataAdmin(long chatId) throws TelegramApiException {

        builderSimpleButtons(chatId, "База клиентов:", "Редактировать массаж:", "Для администратора бота:  ");

//TODO Сделать кнопку 'массаж' : Выбрать массаж и возможность изменить описание, цену, Кнопка 'Сохранить'

    }

    private List buttons(String nameButton1, String nameButton2) {
        List<InlineKeyboardButton> rowButtons = new ArrayList<>();

        rowButtons.add(
                InlineKeyboardButton.builder().
                        text(nameButton1).
                        callbackData(nameButton1).
                        build()
        );
        rowButtons.add(
                InlineKeyboardButton.builder().
                        text(nameButton2).
                        callbackData(nameButton2).
                        build()
        );
        return rowButtons;
    }

    private void dataAdminHandler(Update update) {

        String callbackData = update.getCallbackQuery().getData();
        int messageId = update.getCallbackQuery().getMessage().getMessageId();
        long chatId = update.getCallbackQuery().getMessage().getChatId();

        switch (callbackData) {
            case "База клиентов:" -> {
                try {
                    Iterable<User> allUsers = userRepository.findAll();

                    allUsers.forEach(s -> {
                        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
                        String name = s.getFirstName();


                        rows.add(buttons(name, "Удалить"));

                        try {
                            execute(
                                    SendMessage.builder()
                                            .text("Клиент")
                                            .chatId(chatId)
                                            .replyMarkup(InlineKeyboardMarkup.builder().
                                                    keyboard(rows).
                                                    build())
                                            .build());
                        } catch (TelegramApiException e) {
                            throw new RuntimeException(e);
                        }

                    });

                } catch (NullPointerException e) {
                    log.error(e.getMessage());
                }

            }

            case "Массажи:" -> {
                System.out.println("d");

            }

        }
        Iterable<User> allUsers = userRepository.findAll();
        allUsers.forEach(s -> {
            if (s.getFirstName().equals(callbackData)) {

                try {
                    if (s.getAddress() == null) {
                        s.setAddress("❌");

                    }
                    String name = s.getFirstName();
                    String address = s.getAddress();
                    String numberPhone = s.getNumberPhone();
                    String registered = s.getRegistered().toString().substring(0, 16);
                    Long idClient = s.getId();

                    Set<Appointment> appointmentByUserId = appointmentRepository.getAppointmentByUserId(idClient);

                    if (s.getNumberPhone() == null) {
                        s.setNumberPhone("❌");

                    } else {
                        if (appointmentByUserId.isEmpty()) {

                            String date = "❌";
                            String massageName = "❌";
                            String time = "❌";

                            sendMessage(chatId, name + '\n'
                                    + "\uD83C\uDFE0  Адрес: " + address + '\n'
                                    + "\uD83D\uDCDE  Номер телефона: " + numberPhone + '\n'
                                    + "\uD83D\uDCC5  Дата регистрации: " + registered + '\n'
                                    + "\uD83D\uDDD3  Дата записи на масаж: " + date + '\n'
                                    + "\uD83D\uDD54  Запись на: " + time + " ч" + '\n'
                                    + "\uD83D\uDC86\u200D♀️  Вид массажа:  " + massageName);
                        } else {
                            appointmentByUserId.forEach(x -> {

                                Date dataAppointment = x.getDataAppointment();
                                String date = dataAppointment.toLocaleString();
                                String dateFormat = date.substring(0, date.length() - 17);


                                System.err.println(date);

                                appointmentByUserId.forEach(q -> System.err.println(q.getDataAppointment()));

                                Massage massage = x.getMassage();
                                String massageName = massage.getName();

                                System.err.println(massageName);

                                Integer timeAppointment = x.getTimeAppointment();
                                String time = timeAppointment.toString();

                                System.err.println(time);

                                sendMessage(chatId, name + '\n'
                                        + "\uD83C\uDFE0  Адрес: " + address + '\n'
                                        + "\uD83D\uDCDE  Номер телефона: " + numberPhone + '\n'
                                        + "\uD83D\uDCC5  Дата регистрации: " + registered + '\n'
                                        + "\uD83D\uDDD3  Дата записи на масаж: " + dateFormat + '\n'
                                        + "\uD83D\uDD54  Запись на: " + time + " ч" + '\n'
                                        + "\uD83D\uDC86\u200D♀️  Вид массажа:  " + massageName);

                            });

                        }
                    }

                } catch (LazyInitializationException e) {
                    log.error(e);
                    sendMessage(chatId, "Нет данных");
                }


            }
        });
    }

    private void contactsCommandReceived(long chatId) {
        String answer = "Instagram: https://www.instagram.com/massagirka/" + '\n' +
                "Адресс: г. Минск ул. Мележа д.3 кв. 123";
        sendMessage(chatId, answer);
    }

    private void sendMessage(long chatId, String textSend) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textSend);
        try {
            execute(message);
            log.info(message);
        } catch (TelegramApiException e) {
            log.error(e);
        }

    }

    /**
     * User
     */
    private void registrationUser(Message message) {
        if (userRepository.findById(message.getChatId()).isEmpty()) {
            var chatId = message.getChatId();
            var chat = message.getChat();
            User user = new User();

            user.setId(chatId);
            user.setUserName(chat.getUserName());
            user.setFirstName(chat.getFirstName());
            user.setLastName(chat.getLastName());
            user.setRegistered(new Timestamp(System.currentTimeMillis()));
            user.setPresenceRecord(false);
            userRepository.save(user);
            log.info("user saved " + user);
        }
    }

    void validateAddressFromUser(Update update) {

        String formatAddress = "Укажите свой адрес, в формате: улица, номер дома, номер квартиры " + '\n' +
                "Пример: ул Мележа д 13 кв 18";
        long chatId = update.getCallbackQuery().getMessage().getChatId();
        sendMessage(chatId, formatAddress);
    }

    void validateNumberPhoneFromUser(Long chatId) {

        String formatNumberPhone = "Укажите свой контактный номер телефона в формате:  " + '\n' +
                "8 033 111 11 11";

        sendMessage(chatId, formatNumberPhone);
    }

    //TODO разобраться почему не находит файл по относительному пути
    public void addPhoto(long chatId, String path) {
        File photo = new File(path);
        InputFile inputFile = new InputFile(photo);
        SendPhoto sendPhoto = new SendPhoto(String.valueOf(chatId), inputFile);
        sendPhoto.setCaption("Запись на массаж от MassagIrka");
        try {
            execute(sendPhoto);
        } catch (TelegramApiException e) {
            e.getMessage();
        }
    }

    private void handleCallBackQuery(Update update) throws TelegramApiException {
        String callbackData = update.getCallbackQuery().getData();
        int messageId = update.getCallbackQuery().getMessage().getMessageId();
        long chatId = update.getCallbackQuery().getMessage().getChatId();


        Massage massageCurrent;
        switch (callbackData) {
            case classic -> {
                builderHandlerEditMessageWithButtons(messageId, chatId, selectDescriptionMassage(classic), "Выбрать дату", "Назад");
                massageCurrent = massageRepository.getByName(classic);
                selectMassage.put(chatId, massageCurrent);

            }
            case massageBack -> {
                builderHandlerEditMessageWithButtons(messageId, chatId, selectDescriptionMassage(massageBack), "Выбрать дату", "Назад");
                massageCurrent = massageRepository.getByName(massageBack);
                selectMassage.put(chatId, massageCurrent);
            }
            case anti_cellulite -> {
                builderHandlerEditMessageWithButtons(messageId, chatId, selectDescriptionMassage(anti_cellulite), "Выбрать дату", "Назад");
                massageCurrent = massageRepository.getByName(anti_cellulite);
                selectMassage.put(chatId, massageCurrent);
            }
            case "Выбрать дату" -> selectCommandReceived(chatId, messageId);
            case "Назад" -> buttonsBackSelectMassage(chatId, messageId);

        }
    }

    private void datePickerHandler(Update update) throws TelegramApiException {

        String callbackData = update.getCallbackQuery().getData();
        int messageId = update.getCallbackQuery().getMessage().getMessageId();
        long chatId = update.getCallbackQuery().getMessage().getChatId();
        switch (callbackData) {
            case "1", "2", "3", "4", "5", "6", "7", "8", "9",
                    "10", "11", "12", "13", "14", "15", "16",
                    "17", "18", "19", "20", "21", "22", "23",
                    "24", "25", "26", "27", "28", "29", "30", "31" -> {
                if (Integer.parseInt(callbackData) >= currentDay) {
                    Date dateSelect = new Date(currentYear, currentMonth, Integer.parseInt(callbackData));
                    selectDate.put(chatId, dateSelect);

                    builderHandlerEditMessageWithButtons(messageId, chatId,
                            selectDescriptionAppointment(update),
                            "С выездом", "Без выезда");
                } else sendMessage(chatId, "На эту дату невозможно записаться");

            }
            case "С выездом" -> {
                buttonsSelectTime(chatId, messageId);
                takeOutOrder.put(chatId, true);
            }
            case "Без выезда" -> {
                takeOutOrder.put(chatId, false);
                buttonsSelectTime(chatId, messageId);

            }
        }

    }

    private void builderHandlerEditMessageWithButtons(int messageId, long chatId, String text, String button1, String button2) {
        EditMessageText message = new EditMessageText();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setMessageId(messageId);

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();

        InlineKeyboardButton buttonReturnSelectMassage = new InlineKeyboardButton();
        InlineKeyboardButton buttonSelectDate = new InlineKeyboardButton();

        buttonSelectDate.setText(button1);
        buttonSelectDate.setCallbackData(button1);

        buttonReturnSelectMassage.setText(button2);
        buttonReturnSelectMassage.setCallbackData(button2);

        row.add(buttonSelectDate);
        row.add(buttonReturnSelectMassage);

        rows.add(row);
        markup.setKeyboard(rows);

        message.setReplyMarkup(markup);

        try {
            execute(message);

        } catch (TelegramApiException e) {
            log.error("Error occurred: " + e.getMessage());
        }
    }

    private void builderSimpleButtons(long chatId, String button1, String button2, String text) throws TelegramApiException {

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> rowButtonsConfirmation = new ArrayList<>();

        rowButtonsConfirmation.add(
                InlineKeyboardButton.builder().
                        text(button1).
                        callbackData(button1).
                        build()
        );
        rowButtonsConfirmation.add(
                InlineKeyboardButton.builder().
                        text(button2).
                        callbackData(button2).
                        build()
        );

        rows.add(rowButtonsConfirmation);

        execute(
                SendMessage.builder()
                        .text(text)
                        .chatId(chatId)
                        .replyMarkup(InlineKeyboardMarkup.builder().
                                keyboard(rows).
                                build())
                        .build());
    }

    private void orderDescription(Update update) throws TelegramApiException {
        long chatId = update.getMessage().getChatId();
//        int messageId = update.getMessage().getMessageId();
        String massage = "Вы выбрали " + selectMassage.get(chatId).getName() + " массаж";
        String massagePrice = "Стоимостью:  " + selectMassage.get(chatId).getPrice() + "руб";
        String massageDuration = "Общая продолжительность массажа: " + selectMassage.get(chatId).getDuration();
        String massageTime = "Вы записались на  " + selectTime.get(chatId) + " ч.";
        String address;

        if (takeOutOrder.get(chatId)) {
            try {
                selectAddress.get(chatId);
                address = "Массаж с выездом, по адрессу: " + selectAddress.get(chatId);
            } catch (NullPointerException e) {
                address = "Вы ещё не указали адресс, начните с начала";
            }

        } else address = "Массаж без выезда, по адрессу ул. Мележа д4 кв 3";

        sendMessage(chatId, massage + '\n' + massagePrice + '\n' + massageDuration + '\n' + massageTime + '\n' + address);
        builderSimpleButtons(chatId, "Записаться", "Отмена", "Подтвердите запись");
    }

    private void appointmentInDB(Update update) throws TelegramApiException {
        String callbackData = update.getCallbackQuery().getData();
        long chatId = update.getCallbackQuery().getMessage().getChatId();

        switch (callbackData) {
            case "Записаться" -> {
                sendMessage(chatId, "Вы записались на массаж");
                buttonsSelectMassage(chatId);
                addAppointment(update);
//                sendMessage(293198383,"У вас новая запись на массаж, запись от: ) " + update.getMessage().getChat().getFirstName());
            }

            case "Отмена" -> buttonsSelectMassage(chatId);
        }
    }

    private void timeSelectionHandler(@NotNull Update update) {
        String callbackData = update.getCallbackQuery().getData();
        String text = "Вы выбрали массаж на: " + update.getCallbackQuery().getData() + " ч";
        int messageId = update.getCallbackQuery().getMessage().getMessageId();
        long chatId = update.getCallbackQuery().getMessage().getChatId();
        switch (callbackData) {
            case "10:00", "11:00", "12:00", "13:00", "14:00", "15:00", "16:00", "17:00", "18:00", "19:00" -> {

                if (takeOutOrder.get(chatId)) {
                    builderHandlerEditMessageWithButtons(messageId, chatId, text, "Указать адрес", "Назад");
                    selectTime.put(chatId, callbackData);
                } else {
                    selectTime.put(chatId, callbackData);
                    validateNumberPhoneFromUser(chatId);
                }

            }
            case "Указать адрес" -> validateAddressFromUser(update);
        }

    }

    private String selectDescriptionMassage(String nameMassage) {
        Massage massage = massageRepository.getByName(nameMassage);
        String name = massage.getName();
        String duration = massage.getDuration();
        int price = massage.getPrice();
        String description = massage.getDescription();

        String text = "Вы выбрали массаж: " + name + '\n' +
                "Продолжительность массажа:  " + duration + '\n' +
                "Цена: " + price + " BYN" + '\n' +
                "Что в ходит в массаж: " + description;

        return text;
    }

    private void buttonsBackSelectMassage(long chatId, int messageId) throws TelegramApiException {

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> rowMassageBack = new ArrayList<>();

        rowMassageBack.add(
                InlineKeyboardButton.builder().
                        text(massageBack).
                        callbackData(massageBack).
                        build()
        );
        rowMassageBack.add(
                InlineKeyboardButton.builder().
                        text(classic).
                        callbackData(classic).
                        build()
        );
        rowMassageBack.add(
                InlineKeyboardButton.builder().
                        text(anti_cellulite).
                        callbackData(anti_cellulite).
                        build()
        );
        rows.add(rowMassageBack);

        execute(
                EditMessageText.builder()
                        .text("Выбирите массаж:")
                        .chatId(chatId)
                        .messageId(messageId)
                        .replyMarkup(InlineKeyboardMarkup.builder().
                                keyboard(rows).
                                build())
                        .build());
    }

    private void buttonsSelectMassage(long chatId) throws TelegramApiException {

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> rowMassageBack = new ArrayList<>();

        rowMassageBack.add(
                InlineKeyboardButton.builder().
                        text(massageBack).
                        callbackData(massageBack).
                        build()
        );
        rowMassageBack.add(
                InlineKeyboardButton.builder().
                        text(classic).
                        callbackData(classic).
                        build()
        );
        rowMassageBack.add(
                InlineKeyboardButton.builder().
                        text(anti_cellulite).
                        callbackData(anti_cellulite).
                        build()
        );
        rows.add(rowMassageBack);

        execute(
                SendMessage.builder()
                        .text("Выбирите массаж:")
                        .chatId(chatId)
                        .replyMarkup(InlineKeyboardMarkup.builder().
                                keyboard(rows).
                                build())
                        .build());
    }

    private String selectDescriptionAppointment(Update update) {
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        Date date = selectDate.get(chatId);
        String dateformat = String.format("%1$s %2$td %2$tB", "Дата: ", date);
        Massage massage = selectMassage.get(chatId);
        String nameMassage = massage.getName();
        int priceMassage = massage.getPrice();

        String text = "Вы  выбрали массаж: " + nameMassage + '\n' +
                dateformat + '\n' +
                "Цена массажа: " + priceMassage;
        return text;

    }

    private void addAppointment(Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        try {

            String timeSelect = selectTime.get(chatId).replaceAll(".{3}$", "");

            Appointment appointment = new Appointment();
            appointment.setTimeAppointment(Integer.parseInt(timeSelect));
            appointment.setDataAppointment(selectDate.get(chatId));
            appointment.setMassage(selectMassage.get(chatId));

            Set<Appointment> appointmentSet = new HashSet<>();
            appointmentSet.add(appointment);


            Optional<User> userRepositoryById = userRepository.findById(callbackQuery.getMessage().getChatId());

            User user = userRepositoryById.get();
            user.setPresenceRecord(true);
            user.setAppointmentSet(appointmentSet);
            user.setAddress(selectAddress.get(chatId));
            user.setNumberPhone(selectNumberPhone.get(chatId));
            appointment.setUser(user);

            appointmentRepository.save(appointment);
            userRepository.save(user);
            log.info("appointment saved for: " + user);

        } catch (NullPointerException e) {
            sendMessage(chatId, "Вернитесь в начало, вы не заполнили заказ");
            log.error(e);
        }

    }

    private void buttonsSelectTime(long chatId, int messageId) throws TelegramApiException {
        List<String> timeRecord = new ArrayList<>();
        timeRecord.add("10:00");
        timeRecord.add("11:00");
        timeRecord.add("12:00");
        timeRecord.add("13:00");
        timeRecord.add("15:00");
        timeRecord.add("16:00");
        timeRecord.add("17:00");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> sliderRow = new ArrayList<>();
        for (String time : timeRecord) {

            sliderRow.add(InlineKeyboardButton.builder().
                    callbackData(time).
                    text(time).
                    build());

        }
        rows.add(sliderRow);

        execute(
                EditMessageText.builder()
                        .text("Выбирите время: ")
                        .chatId(chatId)
                        .messageId(messageId)
                        .replyMarkup(markup.builder().
                                keyboard(rows).
                                build())
                        .build());
    }

    private void selectCommandReceived(long chatId, int messageId) throws TelegramApiException {

        int currentYear = Calendar.getInstance().get(Calendar.YEAR);

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> rowList = new ArrayList<>();
        List<InlineKeyboardButton> rowList2 = new ArrayList<>();
        List<InlineKeyboardButton> rowList3 = new ArrayList<>();
        List<InlineKeyboardButton> rowList4 = new ArrayList<>();
        List<InlineKeyboardButton> rowList5 = new ArrayList<>();
//        List<InlineKeyboardButton> rowList0 = new ArrayList<>();

        for (Month month : Month.values()) {

            int monthCurrent = Calendar.getInstance().get(Calendar.MONTH) + 1;
//            Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
            if (monthCurrent == month.getValue()) {
                List<Integer> numberMonth = new ArrayList<>();
//
                rows.add(Collections.singletonList(
                        InlineKeyboardButton.builder().
                                text(month.name()).
                                callbackData("Month" + month.name()).
                                build()
                ));
//                for(String day : dayWek){
//                    rowList0.add((
//                            InlineKeyboardButton.builder().
//                                    text(day).
//                                    callbackData("0").
//                                    build()
//                    ));
//                }

                for (int i = 0; i < month.length(isLeapYear(currentYear)) + 1; i++) {

                    numberMonth.add(i);

                    if (i >= 28) {

                        rowList5.add((
                                InlineKeyboardButton.builder().
                                        text(month.name()).
                                        text(numberMonth.get(i).toString()).
                                        callbackData(i + "").
                                        build()
                        ));
                    }

                    if (i >= 21 && i < 28) {

                        rowList4.add((
                                InlineKeyboardButton.builder().
                                        text(month.name()).
                                        text(numberMonth.get(i).toString()).
                                        callbackData(i + "").
//                                        callbackData(i + month.name()).
        build()
                        ));
                    }
                    if (i >= 15 && i < 22) {

                        rowList3.add((
                                InlineKeyboardButton.builder().
                                        text(month.name()).
                                        text(numberMonth.get(i).toString()).
                                        callbackData(i + "").
                                        build()
                        ));
                    }

                    if (i >= 8 && i < 15) {

                        rowList.add((
                                InlineKeyboardButton.builder().
                                        text(month.name()).
                                        text(numberMonth.get(i).toString()).
                                        callbackData(i + "").
                                        build()
                        ));
                    }

                    if (i > 0 && i <= 7) {

                        rowList2.add((
                                InlineKeyboardButton.builder().
                                        text(month.name()).
                                        text(numberMonth.get(i).toString()).
                                        callbackData(i + "").
                                        build()
                        ));

                    }
                }

            }
        }
//        rows.add(rowList0);
        rows.add(rowList2);
        rows.add(rowList);
        rows.add(rowList3);
        rows.add(rowList4);
        rows.add(rowList5);

        execute(
                EditMessageText.builder()
                        .text("Пожалуйста выбирите дату на которую вы бы хотели записаться:")
                        .chatId(chatId)
                        .messageId(messageId)
                        .replyMarkup(InlineKeyboardMarkup.builder().
                                keyboard(rows).
                                build())
                        .build());
    }

}
