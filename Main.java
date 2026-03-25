import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Sistema de cadastro e notificação de eventos - Console
 * Requisitos atendidos:
 * - Paradigma orientado a objetos
 * - Projeto em console
 * - Cadastro de usuário
 * - Cadastro de eventos com nome, endereço, categoria, horário e descrição
 * - Categorias delimitadas
 * - Consultar eventos e participar
 * - Visualizar eventos com presença confirmada e cancelar participação
 * - Ordenar eventos por horário
 * - Informar se evento está acontecendo, futuro ou já ocorreu
 * - Persistência em arquivo events.data
 * - Carregamento dos eventos ao iniciar o programa
 */
public class Main {

    private static final Scanner scanner = new Scanner(System.in);
    private static final EventManager eventManager = new EventManager("events.data");
    private static User currentUser;

    public static void main(String[] args) {
        eventManager.loadEvents();
        showWelcome();
        registerUser();

        int option;
        do {
            showMenu();
            option = readInt("Escolha uma opção: ");

            switch (option) {
                case 1:
                    createEvent();
                    break;
                case 2:
                    listAllEvents();
                    break;
                case 3:
                    participateInEvent();
                    break;
                case 4:
                    listUserConfirmedEvents();
                    break;
                case 5:
                    cancelParticipation();
                    break;
                case 6:
                    listEventsByStatus();
                    break;
                case 7:
                    showUserData();
                    break;
                case 0:
                    System.out.println("Salvando dados e encerrando...");
                    eventManager.saveEvents();
                    System.out.println("Programa finalizado.");
                    break;
                default:
                    System.out.println("Opção inválida.");
            }

            System.out.println();
        } while (option != 0);
    }

    private static void showWelcome() {
        System.out.println("==============================================");
        System.out.println("   SISTEMA DE CADASTRO E NOTIFICAÇÃO DE EVENTOS");
        System.out.println("==============================================");
        System.out.println("Eventos carregados do arquivo: " + eventManager.getStorageFileName());
        System.out.println();
    }

    private static void registerUser() {
        System.out.println("=== Cadastro do Usuário ===");

        String name = readNonEmpty("Nome: ");
        String email = readNonEmpty("E-mail: ");
        String city = readNonEmpty("Cidade: ");
        String phone = readNonEmpty("Telefone: ");
        int age = readInt("Idade: ");

        currentUser = new User(name, email, city, phone, age);

        System.out.println("\nUsuário cadastrado com sucesso!");
    }

    private static void showMenu() {
        System.out.println("============== MENU ==============");
        System.out.println("1 - Cadastrar evento");
        System.out.println("2 - Listar todos os eventos");
        System.out.println("3 - Confirmar participação em evento");
        System.out.println("4 - Ver meus eventos confirmados");
        System.out.println("5 - Cancelar participação");
        System.out.println("6 - Ver eventos por status (futuros / acontecendo / ocorridos)");
        System.out.println("7 - Exibir dados do usuário");
        System.out.println("0 - Sair");
        System.out.println("==================================");
    }

    private static void createEvent() {
        System.out.println("=== Cadastro de Evento ===");

        String name = readNonEmpty("Nome do evento: ");
        String address = readNonEmpty("Endereço: ");

        EventCategory category = chooseCategory();

        LocalDateTime dateTime = readDateTime(
                "Data e hora do evento (formato: dd/MM/yyyy HH:mm): "
        );

        String description = readNonEmpty("Descrição: ");

        Event event = new Event(name, address, category, dateTime, description);
        eventManager.addEvent(event);
        eventManager.saveEvents();

        System.out.println("Evento cadastrado com sucesso!");
    }

    private static void listAllEvents() {
        System.out.println("=== Lista de Eventos ===");

        List<Event> events = eventManager.getAllEventsSorted();

        if (events.isEmpty()) {
            System.out.println("Nenhum evento cadastrado.");
            return;
        }

        for (int i = 0; i < events.size(); i++) {
            System.out.println("[" + (i + 1) + "]");
            System.out.println(events.get(i).toDisplayString());
            System.out.println("----------------------------------");
        }
    }

    private static void participateInEvent() {
        System.out.println("=== Confirmar Participação ===");

        List<Event> events = eventManager.getAllEventsSorted();
        if (events.isEmpty()) {
            System.out.println("Não há eventos cadastrados.");
            return;
        }

        listAllEvents();
        int index = readInt("Digite o número do evento para participar: ") - 1;

        if (!isValidIndex(index, events.size())) {
            System.out.println("Evento inválido.");
            return;
        }

        Event selectedEvent = events.get(index);

        if (selectedEvent.hasParticipant(currentUser.getEmail())) {
            System.out.println("Você já confirmou presença neste evento.");
            return;
        }

        selectedEvent.addParticipant(currentUser.getEmail());
        eventManager.saveEvents();

        System.out.println("Participação confirmada com sucesso!");
    }

    private static void listUserConfirmedEvents() {
        System.out.println("=== Meus Eventos Confirmados ===");

        List<Event> confirmed = eventManager.getEventsByParticipant(currentUser.getEmail());

        if (confirmed.isEmpty()) {
            System.out.println("Você ainda não confirmou presença em nenhum evento.");
            return;
        }

        confirmed.sort(Comparator.comparing(Event::getDateTime));

        for (int i = 0; i < confirmed.size(); i++) {
            System.out.println("[" + (i + 1) + "]");
            System.out.println(confirmed.get(i).toDisplayString());
            System.out.println("----------------------------------");
        }
    }

    private static void cancelParticipation() {
        System.out.println("=== Cancelar Participação ===");

        List<Event> confirmed = eventManager.getEventsByParticipant(currentUser.getEmail());

        if (confirmed.isEmpty()) {
            System.out.println("Você não possui participações confirmadas.");
            return;
        }

        for (int i = 0; i < confirmed.size(); i++) {
            System.out.println("[" + (i + 1) + "]");
            System.out.println(confirmed.get(i).toDisplayString());
            System.out.println("----------------------------------");
        }

        int index = readInt("Digite o número do evento para cancelar: ") - 1;

        if (!isValidIndex(index, confirmed.size())) {
            System.out.println("Evento inválido.");
            return;
        }

        Event selectedEvent = confirmed.get(index);
        selectedEvent.removeParticipant(currentUser.getEmail());
        eventManager.saveEvents();

        System.out.println("Participação cancelada com sucesso!");
    }

    private static void listEventsByStatus() {
        System.out.println("=== Eventos por Status ===");

        List<Event> events = eventManager.getAllEventsSorted();

        if (events.isEmpty()) {
            System.out.println("Nenhum evento cadastrado.");
            return;
        }

        System.out.println("\n--- EVENTOS ACONTECENDO AGORA ---");
        printEvents(eventManager.getOngoingEvents());

        System.out.println("\n--- PRÓXIMOS EVENTOS ---");
        printEvents(eventManager.getUpcomingEvents());

        System.out.println("\n--- EVENTOS JÁ OCORRIDOS ---");
        printEvents(eventManager.getPastEvents());
    }

    private static void printEvents(List<Event> events) {
        if (events.isEmpty()) {
            System.out.println("Nenhum evento nesta categoria.");
            return;
        }

        for (Event event : events) {
            System.out.println(event.toDisplayString());
            System.out.println("----------------------------------");
        }
    }

    private static void showUserData() {
        System.out.println("=== Dados do Usuário ===");
        System.out.println(currentUser);
    }

    private static EventCategory chooseCategory() {
        System.out.println("Categorias disponíveis:");
        EventCategory[] categories = EventCategory.values();

        for (int i = 0; i < categories.length; i++) {
            System.out.println((i + 1) + " - " + categories[i].getDisplayName());
        }

        int option;
        do {
            option = readInt("Escolha a categoria: ");
            if (option < 1 || option > categories.length) {
                System.out.println("Categoria inválida.");
            }
        } while (option < 1 || option > categories.length);

        return categories[option - 1];
    }

    private static LocalDateTime readDateTime(String message) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        while (true) {
            try {
                System.out.print(message);
                String input = scanner.nextLine();
                return LocalDateTime.parse(input, formatter);
            } catch (DateTimeParseException e) {
                System.out.println("Formato inválido. Use: dd/MM/yyyy HH:mm");
            }
        }
    }

    private static String readNonEmpty(String message) {
        while (true) {
            System.out.print(message);
            String input = scanner.nextLine().trim();
            if (!input.isEmpty()) {
                return input;
            }
            System.out.println("Campo obrigatório.");
        }
    }

    private static int readInt(String message) {
        while (true) {
            try {
                System.out.print(message);
                return Integer.parseInt(scanner.nextLine());
            } catch (NumberFormatException e) {
                System.out.println("Digite um número válido.");
            }
        }
    }

    private static boolean isValidIndex(int index, int size) {
        return index >= 0 && index < size;
    }
}

class User {
    private String name;
    private String email;
    private String city;
    private String phone;
    private int age;

    public User(String name, String email, String city, String phone, int age) {
        this.name = name;
        this.email = email;
        this.city = city;
        this.phone = phone;
        this.age = age;
    }

    public String getEmail() {
        return email;
    }

    @Override
    public String toString() {
        return "Nome: " + name + "\n" +
               "E-mail: " + email + "\n" +
               "Cidade: " + city + "\n" +
               "Telefone: " + phone + "\n" +
               "Idade: " + age;
    }
}

enum EventCategory {
    FESTA("Festa"),
    ESPORTIVO("Esportivo"),
    SHOW("Show"),
    PALESTRA("Palestra"),
    FEIRA("Feira"),
    CULTURAL("Cultural"),
    OUTRO("Outro");

    private final String displayName;

    EventCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

class Event {
    private static int counter = 1;

    private int id;
    private String name;
    private String address;
    private EventCategory category;
    private LocalDateTime dateTime;
    private String description;
    private Set<String> participants;

    public Event(String name, String address, EventCategory category, LocalDateTime dateTime, String description) {
        this.id = counter++;
        this.name = name;
        this.address = address;
        this.category = category;
        this.dateTime = dateTime;
        this.description = description;
        this.participants = new HashSet<>();
    }

    public Event(int id, String name, String address, EventCategory category, LocalDateTime dateTime,
                 String description, Set<String> participants) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.category = category;
        this.dateTime = dateTime;
        this.description = description;
        this.participants = participants != null ? participants : new HashSet<>();

        if (id >= counter) {
            counter = id + 1;
        }
    }

    public int getId() {
        return id;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public void addParticipant(String email) {
        participants.add(email);
    }

    public void removeParticipant(String email) {
        participants.remove(email);
    }

    public boolean hasParticipant(String email) {
        return participants.contains(email);
    }

    public boolean isOngoing() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime end = dateTime.plusHours(2); // duração estimada de 2h
        return (now.isEqual(dateTime) || now.isAfter(dateTime)) && now.isBefore(end);
    }

    public boolean isPast() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime end = dateTime.plusHours(2);
        return now.isAfter(end);
    }

    public boolean isUpcoming() {
        return LocalDateTime.now().isBefore(dateTime);
    }

    public String getStatus() {
        if (isOngoing()) {
            return "ACONTECENDO AGORA";
        } else if (isUpcoming()) {
            return "PRÓXIMO EVENTO";
        } else {
            return "JÁ OCORREU";
        }
    }

    public String toFileString() {
        String participantsText = String.join(",", participants);
        return id + ";" +
               escape(name) + ";" +
               escape(address) + ";" +
               category.name() + ";" +
               dateTime + ";" +
               escape(description) + ";" +
               escape(participantsText);
    }

    public static Event fromFileString(String line) {
        String[] parts = splitPreservingEscapes(line);

        if (parts.length < 7) {
            throw new IllegalArgumentException("Linha inválida no arquivo: " + line);
        }

        int id = Integer.parseInt(parts[0]);
        String name = unescape(parts[1]);
        String address = unescape(parts[2]);
        EventCategory category = EventCategory.valueOf(parts[3]);
        LocalDateTime dateTime = LocalDateTime.parse(parts[4]);
        String description = unescape(parts[5]);
        String participantsRaw = unescape(parts[6]);

        Set<String> participants = new HashSet<>();
        if (!participantsRaw.isBlank()) {
            participants.addAll(Arrays.asList(participantsRaw.split(",")));
        }

        return new Event(id, name, address, category, dateTime, description, participants);
    }

    public String toDisplayString() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        return "ID: " + id + "\n" +
               "Nome: " + name + "\n" +
               "Endereço: " + address + "\n" +
               "Categoria: " + category.getDisplayName() + "\n" +
               "Horário: " + dateTime.format(formatter) + "\n" +
               "Descrição: " + description + "\n" +
               "Participantes confirmados: " + participants.size() + "\n" +
               "Status: " + getStatus();
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace(";", "\\;");
    }

    private static String unescape(String value) {
        StringBuilder result = new StringBuilder();
        boolean escaping = false;

        for (char c : value.toCharArray()) {
            if (escaping) {
                result.append(c);
                escaping = false;
            } else if (c == '\\') {
                escaping = true;
            } else {
                result.append(c);
            }
        }

        return result.toString();
    }

    private static String[] splitPreservingEscapes(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean escaping = false;

        for (char c : line.toCharArray()) {
            if (escaping) {
                current.append(c);
                escaping = false;
            } else if (c == '\\') {
                current.append(c);
                escaping = true;
            } else if (c == ';') {
                fields.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }

        fields.add(current.toString());
        return fields.toArray(new String[0]);
    }
}

class EventManager {
    private final List<Event> events;
    private final String storageFileName;

    public EventManager(String storageFileName) {
        this.events = new ArrayList<>();
        this.storageFileName = storageFileName;
    }

    public String getStorageFileName() {
        return storageFileName;
    }

    public void addEvent(Event event) {
        events.add(event);
    }

    public List<Event> getAllEventsSorted() {
        List<Event> sorted = new ArrayList<>(events);
        sorted.sort(Comparator.comparing(Event::getDateTime));
        return sorted;
    }

    public List<Event> getEventsByParticipant(String email) {
        List<Event> result = new ArrayList<>();
        for (Event event : events) {
            if (event.hasParticipant(email)) {
                result.add(event);
            }
        }
        result.sort(Comparator.comparing(Event::getDateTime));
        return result;
    }

    public List<Event> getUpcomingEvents() {
        List<Event> result = new ArrayList<>();
        for (Event event : getAllEventsSorted()) {
            if (event.isUpcoming()) {
                result.add(event);
            }
        }
        return result;
    }

    public List<Event> getOngoingEvents() {
        List<Event> result = new ArrayList<>();
        for (Event event : getAllEventsSorted()) {
            if (event.isOngoing()) {
                result.add(event);
            }
        }
        return result;
    }

    public List<Event> getPastEvents() {
        List<Event> result = new ArrayList<>();
        for (Event event : getAllEventsSorted()) {
            if (event.isPast()) {
                result.add(event);
            }
        }
        return result;
    }

    public void saveEvents() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(storageFileName))) {
            for (Event event : events) {
                writer.write(event.toFileString());
                writer.newLine();
            }
        } catch (IOException e) {
            System.out.println("Erro ao salvar eventos: " + e.getMessage());
        }
    }

    public void loadEvents() {
        File file = new File(storageFileName);

        if (!file.exists()) {
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isBlank()) {
                    events.add(Event.fromFileString(line));
                }
            }
        } catch (IOException e) {
            System.out.println("Erro ao carregar eventos: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Erro ao interpretar dados do arquivo: " + e.getMessage());
        }
    }
}