package pro.sky.telegrambot.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pro.sky.telegrambot.model.NotificationTask;
import pro.sky.telegrambot.repository.NotificationTaskRepository;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TelegramBotUpdatesListener implements UpdatesListener {

    private Logger logger = LoggerFactory.getLogger(TelegramBotUpdatesListener.class);

    @Autowired
    private TelegramBot telegramBot;

    @Autowired
    private NotificationTaskRepository notificationTaskRepository;

    private static final Pattern MESSAGE_PATTERN = Pattern.compile("(\\d{2}\\.\\d{2}\\.\\d{4} \\d{2}:\\d{2}) (.*)");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    @PostConstruct
    public void init() {
        telegramBot.setUpdatesListener(this);
    }

    @Override
    public int process(List<Update> updates) {
        updates.forEach(update -> {
            logger.info("Обработка обновления: {}", update);
            if (update.message() != null && update.message().text() != null) {
                String messageText = update.message().text();
                Long chatId = update.message().chat().id();

                if (messageText.equals("/start")) {
                    SendMessage message = new SendMessage(chatId, "Добро пожаловать в бот уведомлений! Отправьте задачу в формате: ДД.ММ.ГГГГ ЧЧ:ММ Описание задачи");
                    telegramBot.execute(message);
                } else {
                    Matcher matcher = MESSAGE_PATTERN.matcher(messageText);
                    if (matcher.matches()) {
                        try {
                            String dateTimeStr = matcher.group(1);
                            String taskMessage = matcher.group(2);
                            LocalDateTime dateTime = LocalDateTime.parse(dateTimeStr, DATE_TIME_FORMATTER);

                            NotificationTask task = new NotificationTask(chatId, taskMessage, dateTime);
                            notificationTaskRepository.save(task);
                            SendMessage confirmation = new SendMessage(chatId, "Задача запланирована: " + taskMessage + " на " + dateTimeStr);
                            telegramBot.execute(confirmation);
                        } catch (Exception e) {
                            logger.error("Ошибка парсинга сообщения: {}", messageText, e);
                            SendMessage error = new SendMessage(chatId, "Неверный формат. Используйте: ДД.ММ.ГГГГ ЧЧ:ММ Описание задачи");
                            telegramBot.execute(error);
                        }
                    } else {
                        SendMessage error = new SendMessage(chatId, "Неверный формат. Используйте: ДД.ММ.ГГГГ ЧЧ:ММ Описание задачи");
                        telegramBot.execute(error);
                    }
                }
            }
        });
        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }
}