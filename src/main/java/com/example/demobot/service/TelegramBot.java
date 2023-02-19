package com.example.demobot.service;

import com.example.demobot.config.BotConfig;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.Month;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

@Component
@Log4j
@RequiredArgsConstructor
public class TelegramBot extends TelegramLongPollingBot {

    private final BotConfig config;

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
            long chatId = update.getMessage().getChatId();
            log.info(messageText);

            switch (messageText) {
                case "/start":

                    startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                    break;
                case "/contacts":
                    contactsCommandReceived(chatId);
                    break;
                case "/select":
                    selectCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                default:
                    sendMessage(chatId, "К этому меня жинь не готовила");
            }
        }

    }

    private void startCommandReceived(long chatId, String name) {
        String answer = "Привет, " + name + ", рад познакомиться";
        sendMessage(chatId, answer);
    }

    private void contactsCommandReceived(long chatId) {
        String answer = "Instagram: https://www.instagram.com/massagirka/";
        sendMessage(chatId, answer);
    }

    private void selectCommandReceived(long chatId, String name) throws TelegramApiException {
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();

        for (Month month : Month.values()) {

            int monthCurrent = Calendar.getInstance().get(Calendar.MONTH) + 1;

            if (monthCurrent == month.getValue()) {
                List<Integer> numberMonth = new ArrayList<>();

                buttons.add(Arrays.asList(
                        InlineKeyboardButton.builder().
                                text(month.name()).
                                callbackData("0" + month.name()).
                                build()

                ));
                for (int i = 0; i < month.length(isLeapYear(currentYear)); i++) { //начать отсчет с 1

                    numberMonth.add(i);

                    buttons.add(Arrays.asList(
                            InlineKeyboardButton.builder().
                                    text(month.name()).
                                    text(numberMonth.get(i).toString()).
                                    callbackData("0" + month.name()).
                                    build()
                    ));

                }

            }
        }
        execute(
                SendMessage.builder()
                        .text("Пожалуйста выбирите дату на которую вы бы хотели записаться:")
                        .chatId(chatId)
                        .replyMarkup(InlineKeyboardMarkup.builder().keyboard(buttons).build())
                        .build());
        return;
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

    private boolean isLeapYear(int year) {
        boolean leapYear = false;
        if ((year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)) {
            leapYear = true;
        }
        return leapYear;
    }

}
