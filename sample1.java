import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * A sample Library Management System demonstrating core Java concepts:
 * classes, enums, interfaces, custom exceptions, collections, streams,
 * the java.time API, and basic object-oriented design.
 */
public class LibraryManagementSystem {

    // ------------------------------------------------------------------
    // Enums
    // ------------------------------------------------------------------
    enum Genre {
        FICTION, SCIENCE, HISTORY, TECHNOLOGY, BIOGRAPHY
    }

    enum MembershipType {
        STANDARD(3, 14, 1.0),
        PREMIUM(10, 30, 0.5);

        private final int maxBooks;
        private final int loanDays;
        private final double finePerDay;

        MembershipType(int maxBooks, int loanDays, double finePerDay) {
            this.maxBooks = maxBooks;
            this.loanDays = loanDays;
            this.finePerDay = finePerDay;
        }

        public int getMaxBooks() { return maxBooks; }
        public int getLoanDays() { return loanDays; }
        public double getFinePerDay() { return finePerDay; }
    }

    // ------------------------------------------------------------------
    // Custom exceptions
    // ------------------------------------------------------------------
    static class LibraryException extends Exception {
        public LibraryException(String message) {
            super(message);
        }
    }

    static class BookNotAvailableException extends LibraryException {
        public BookNotAvailableException(String title) {
            super("Book is not available: " + title);
        }
    }

    static class LoanLimitExceededException extends LibraryException {
        public LoanLimitExceededException(String member, int limit) {
            super(member + " has reached the loan limit of " + limit + " books");
        }
    }

    // ------------------------------------------------------------------
    // Interface
    // ------------------------------------------------------------------
    interface Searchable {
        List<Book> searchByTitle(String keyword);
        List<Book> searchByAuthor(String author);
        List<Book> searchByGenre(Genre genre);
    }

    // ------------------------------------------------------------------
    // Domain classes
    // ------------------------------------------------------------------
    static class Book {
        private final String isbn;
        private final String title;
        private final String author;
        private final Genre genre;
        private final int year;
        private boolean available = true;

        public Book(String isbn, String title, String author, Genre genre, int year) {
            this.isbn = isbn;
            this.title = title;
            this.author = author;
            this.genre = genre;
            this.year = year;
        }

        public String getIsbn() { return isbn; }
        public String getTitle() { return title; }
        public String getAuthor() { return author; }
        public Genre getGenre() { return genre; }
        public int getYear() { return year; }
        public boolean isAvailable() { return available; }
        public void setAvailable(boolean available) { this.available = available; }

        @Override
        public String toString() {
            return String.format("[%s] %-32s by %-22s (%d, %s) %s",
                    isbn, title, author, year, genre,
                    available ? "Available" : "On loan");
        }
    }

    static class Member {
        private final int id;
        private final String name;
        private final MembershipType type;
        private final List<Loan> activeLoans = new ArrayList<>();
        private double outstandingFines = 0.0;

        public Member(int id, String name, MembershipType type) {
            this.id = id;
            this.name = name;
            this.type = type;
        }

        public int getId() { return id; }
        public String getName() { return name; }
        public MembershipType getType() { return type; }
        public List<Loan> getActiveLoans() { return activeLoans; }
        public double getOutstandingFines() { return outstandingFines; }

        public void addFine(double amount) { this.outstandingFines += amount; }

        public void payFines() { this.outstandingFines = 0.0; }

        public boolean canBorrow() {
            return activeLoans.size() < type.getMaxBooks();
        }

        @Override
        public String toString() {
            return String.format("Member #%d: %s (%s) - %d active loan(s), fines: %.2f",
                    id, name, type, activeLoans.size(), outstandingFines);
        }
    }

    static class Loan {
        private final Book book;
        private final Member member;
        private final LocalDate issueDate;
        private final LocalDate dueDate;
        private LocalDate returnDate;

        public Loan(Book book, Member member, LocalDate issueDate) {
            this.book = book;
            this.member = member;
            this.issueDate = issueDate;
            this.dueDate = issueDate.plusDays(member.getType().getLoanDays());
        }

        public Book getBook() { return book; }
        public Member getMember() { return member; }
        public LocalDate getIssueDate() { return issueDate; }
        public LocalDate getDueDate() { return dueDate; }
        public LocalDate getReturnDate() { return returnDate; }
        public void setReturnDate(LocalDate returnDate) { this.returnDate = returnDate; }

        public long daysOverdue(LocalDate asOf) {
            LocalDate end = (returnDate != null) ? returnDate : asOf;
            return Math.max(0, ChronoUnit.DAYS.between(dueDate, end));
        }

        @Override
        public String toString() {
            return String.format("'%s' -> %s | issued %s, due %s%s",
                    book.getTitle(), member.getName(), issueDate, dueDate,
                    returnDate != null ? ", returned " + returnDate : "");
        }
    }

    // ------------------------------------------------------------------
    // Library service
    // ------------------------------------------------------------------
    static class Library implements Searchable {
        private final Map<String, Book> catalog = new HashMap<>();
        private final Map<Integer, Member> members = new HashMap<>();
        private final List<Loan> loanHistory = new ArrayList<>();

        public void addBook(Book book) {
            catalog.put(book.getIsbn(), book);
        }

        public void registerMember(Member member) {
            members.put(member.getId(), member);
        }

        public Optional<Member> findMember(int id) {
            return Optional.ofNullable(members.get(id));
        }

        public Optional<Book> findBook(String isbn) {
            return Optional.ofNullable(catalog.get(isbn));
        }

        public Loan checkoutBook(int memberId, String isbn, LocalDate date)
                throws LibraryException {
            Member member = findMember(memberId)
                    .orElseThrow(() -> new LibraryException("Unknown member id: " + memberId));
            Book book = findBook(isbn)
                    .orElseThrow(() -> new LibraryException("Unknown ISBN: " + isbn));

            if (!book.isAvailable()) {
                throw new BookNotAvailableException(book.getTitle());
            }
            if (!member.canBorrow()) {
                throw new LoanLimitExceededException(member.getName(),
                        member.getType().getMaxBooks());
            }
            if (member.getOutstandingFines() > 10.0) {
                throw new LibraryException(member.getName()
                        + " must clear fines before borrowing");
            }

            Loan loan = new Loan(book, member, date);
            book.setAvailable(false);
            member.getActiveLoans().add(loan);
            loanHistory.add(loan);
            return loan;
        }

        public double returnBook(int memberId, String isbn, LocalDate date)
                throws LibraryException {
            Member member = findMember(memberId)
                    .orElseThrow(() -> new LibraryException("Unknown member id: " + memberId));

            Loan loan = member.getActiveLoans().stream()
                    .filter(l -> l.getBook().getIsbn().equals(isbn))
                    .findFirst()
                    .orElseThrow(() -> new LibraryException(
                            member.getName() + " has not borrowed ISBN " + isbn));

            loan.setReturnDate(date);
            loan.getBook().setAvailable(true);
            member.getActiveLoans().remove(loan);

            long overdue = loan.daysOverdue(date);
            double fine = overdue * member.getType().getFinePerDay();
            if (fine > 0) {
                member.addFine(fine);
            }
            return fine;
        }

        @Override
        public List<Book> searchByTitle(String keyword) {
            String k = keyword.toLowerCase();
            return catalog.values().stream()
                    .filter(b -> b.getTitle().toLowerCase().contains(k))
                    .sorted(Comparator.comparing(Book::getTitle))
                    .collect(Collectors.toList());
        }

        @Override
        public List<Book> searchByAuthor(String author) {
            String a = author.toLowerCase();
            return catalog.values().stream()
                    .filter(b -> b.getAuthor().toLowerCase().contains(a))
                    .sorted(Comparator.comparing(Book::getTitle))
                    .collect(Collectors.toList());
        }

        @Override
        public List<Book> searchByGenre(Genre genre) {
            return catalog.values().stream()
                    .filter(b -> b.getGenre() == genre)
                    .sorted(Comparator.comparingInt(Book::getYear))
                    .collect(Collectors.toList());
        }

        public Map<Genre, Long> countBooksByGenre() {
            return catalog.values().stream()
                    .collect(Collectors.groupingBy(Book::getGenre, Collectors.counting()));
        }

        public List<Loan> getOverdueLoans(LocalDate asOf) {
            return members.values().stream()
                    .flatMap(m -> m.getActiveLoans().stream())
                    .filter(l -> l.daysOverdue(asOf) > 0)
                    .collect(Collectors.toList());
        }

        public List<Loan> getLoanHistory() {
            return loanHistory;
        }

        public List<Book> getAllBooks() {
            return catalog.values().stream()
                    .sorted(Comparator.comparing(Book::getIsbn))
                    .collect(Collectors.toList());
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------
    private static void printHeader(String text) {
        System.out.println();
        System.out.println("=== " + text + " ===");
    }

    private static void seedLibrary(Library library) {
        library.addBook(new Book("101", "Clean Code", "Robert C. Martin", Genre.TECHNOLOGY, 2008));
        library.addBook(new Book("102", "Effective Java", "Joshua Bloch", Genre.TECHNOLOGY, 2018));
        library.addBook(new Book("103", "A Brief History of Time", "Stephen Hawking", Genre.SCIENCE, 1988));
        library.addBook(new Book("104", "Cosmos", "Carl Sagan", Genre.SCIENCE, 1980));
        library.addBook(new Book("105", "Sapiens", "Yuval Noah Harari", Genre.HISTORY, 2011));
        library.addBook(new Book("106", "The Guns of August", "Barbara Tuchman", Genre.HISTORY, 1962));
        library.addBook(new Book("107", "1984", "George Orwell", Genre.FICTION, 1949));
        library.addBook(new Book("108", "To Kill a Mockingbird", "Harper Lee", Genre.FICTION, 1960));
        library.addBook(new Book("109", "Steve Jobs", "Walter Isaacson", Genre.BIOGRAPHY, 2011));
        library.addBook(new Book("110", "Wings of Fire", "A. P. J. Abdul Kalam", Genre.BIOGRAPHY, 1999));

        library.registerMember(new Member(1, "Anit", MembershipType.PREMIUM));
        library.registerMember(new Member(2, "Riya", MembershipType.STANDARD));
        library.registerMember(new Member(3, "Kiran", MembershipType.STANDARD));
    }

    // ------------------------------------------------------------------
    // Main demo
    // ------------------------------------------------------------------
    public static void main(String[] args) {
        Library library = new Library();
        seedLibrary(library);

        printHeader("Full catalog");
        library.getAllBooks().forEach(System.out::println);

        printHeader("Search: titles containing 'history'");
        library.searchByTitle("history").forEach(System.out::println);

        printHeader("Search: author 'carl'");
        library.searchByAuthor("carl").forEach(System.out::println);

        printHeader("Search: genre TECHNOLOGY (sorted by year)");
        library.searchByGenre(Genre.TECHNOLOGY).forEach(System.out::println);

        printHeader("Books per genre");
        library.countBooksByGenre().forEach((g, c) ->
                System.out.printf("%-12s %d%n", g, c));

        LocalDate today = LocalDate.of(2026, 1, 10);

        printHeader("Checkouts");
        try {
            System.out.println(library.checkoutBook(1, "101", today));
            System.out.println(library.checkoutBook(2, "103", today));
            System.out.println(library.checkoutBook(2, "104", today));
            System.out.println(library.checkoutBook(2, "105", today));
            // This 4th checkout for a STANDARD member should fail
            System.out.println(library.checkoutBook(2, "106", today));
        } catch (LibraryException e) {
            System.out.println("Checkout failed: " + e.getMessage());
        }

        printHeader("Attempt to borrow an already-loaned book");
        try {
            library.checkoutBook(3, "101", today);
        } catch (LibraryException e) {
            System.out.println("Checkout failed: " + e.getMessage());
        }

        printHeader("Overdue check (30 days later)");
        LocalDate later = today.plusDays(30);
        List<Loan> overdue = library.getOverdueLoans(later);
        if (overdue.isEmpty()) {
            System.out.println("No overdue loans.");
        } else {
            overdue.forEach(l -> System.out.printf("%s (%d days overdue)%n",
                    l, l.daysOverdue(later)));
        }

        printHeader("Returns");
        try {
            double fine1 = library.returnBook(2, "103", later);
            System.out.printf("Riya returned 'A Brief History of Time', fine: %.2f%n", fine1);

            double fine2 = library.returnBook(1, "101", later);
            System.out.printf("Anit returned 'Clean Code', fine: %.2f%n", fine2);

            // Returning a book that was never borrowed
            library.returnBook(3, "999", later);
        } catch (LibraryException e) {
            System.out.println("Return failed: " + e.getMessage());
        }

        printHeader("Member status");
        for (int id = 1; id <= 3; id++) {
            library.findMember(id).ifPresent(System.out::println);
        }

        printHeader("Fines blocking new checkouts");
        library.findMember(2).ifPresent(m -> m.addFine(15.0));
        try {
            library.checkoutBook(2, "107", later);
        } catch (LibraryException e) {
            System.out.println("Checkout failed: " + e.getMessage());
        }
        library.findMember(2).ifPresent(Member::payFines);
        try {
            System.out.println(library.checkoutBook(2, "107", later));
        } catch (LibraryException e) {
            System.out.println("Checkout failed: " + e.getMessage());
        }

        printHeader("Loan history");
        library.getLoanHistory().forEach(System.out::println);

        System.out.println();
        System.out.println("Total loans recorded: " + library.getLoanHistory().size());
    }
}
