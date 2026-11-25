import java.io.*;
import java.util.Scanner;

/**
 * Online Quiz System (PS No. 3)
 * Concepts: Queue, Stack, Array, Sorting, Methods, File Handling
 */
public class Main {

    // ---------- Data classes ----------
    static class Question {
        String text;
        String[] options = new String[4];
        char answer;
    }

    static class AnswerRecord {
        int qNo;
        char given;
        char correct;
        AnswerRecord(int qNo, char given, char correct) {
            this.qNo = qNo; this.given = given; this.correct = correct;
        }
    }

    static class ScoreEntry {
        String name;
        int score;
        int total;
        ScoreEntry(String name, int score, int total) {
            this.name = name; this.score = score; this.total = total;
        }
    }

    // ---------- Array based Queue (FIFO) ----------
    static class QuestionQueue {
        private Question[] data;
        private int front = 0, size = 0;
        QuestionQueue(int capacity) { data = new Question[Math.max(capacity, 1)]; }
        void enqueue(Question q) {
            if (size == data.length) throw new IllegalStateException("Queue full");
            data[(front + size) % data.length] = q;
            size++;
        }
        Question dequeue() {
            if (size == 0) return null;
            Question q = data[front];
            front = (front + 1) % data.length;
            size--;
            return q;
        }
        Question peek() { return size == 0 ? null : data[front]; }
        boolean isEmpty() { return size == 0; }
    }

    // ---------- Array based Stack (LIFO) ----------
    static class AnswerStack {
        private AnswerRecord[] data;
        private int top = -1;
        AnswerStack(int capacity) { data = new AnswerRecord[Math.max(capacity, 1)]; }
        void push(AnswerRecord r) {
            if (top == data.length - 1) throw new IllegalStateException("Stack full");
            data[++top] = r;
        }
        AnswerRecord pop() { return top == -1 ? null : data[top--]; }
        boolean isEmpty() { return top == -1; }
        void clear() { top = -1; }
        int size() { return top + 1; }
        AnswerRecord[] topToBottom() {       // latest first, without destroying the stack
            AnswerRecord[] out = new AnswerRecord[size()];
            for (int i = 0; i <= top; i++) out[i] = data[top - i];
            return out;
        }
    }

    // ---------- Global state ----------
    static final String QUIZ_FILE = "quiz.txt";
    static final String SCORE_FILE = "scores.txt";
    static final int MAX_QUESTIONS = 100;
    static final int MAX_SCORES = 1000;

    static Question[] questionBank = new Question[MAX_QUESTIONS];
    static int questionCount = 0;
    static ScoreEntry[] scores = new ScoreEntry[MAX_SCORES];
    static int scoreCount = 0;
    static AnswerStack answerStack = new AnswerStack(MAX_QUESTIONS);
    static Scanner sc;

    // ---------- File handling ----------
    // quiz.txt line format:  question|optionA|optionB|optionC|optionD|answer
    static void loadQuestions() {
        try (BufferedReader br = new BufferedReader(new FileReader(QUIZ_FILE))) {
            String line;
            while ((line = br.readLine()) != null && questionCount < MAX_QUESTIONS) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] p = line.split("\\|");
                if (p.length < 6) continue;
                Question q = new Question();
                q.text = p[0].trim();
                for (int i = 0; i < 4; i++) q.options[i] = p[i + 1].trim();
                q.answer = Character.toUpperCase(p[5].trim().charAt(0));
                questionBank[questionCount++] = q;
            }
        } catch (IOException e) {
            System.out.println("Could not read " + QUIZ_FILE + ": " + e.getMessage());
        }
    }

    // scores.txt line format:  name,score,total
    static void loadScores() {
        File f = new File(SCORE_FILE);
        if (!f.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null && scoreCount < MAX_SCORES) {
                String[] p = line.trim().split(",");
                if (p.length < 3) continue;
                try {
                    scores[scoreCount++] = new ScoreEntry(p[0].trim(),
                            Integer.parseInt(p[1].trim()), Integer.parseInt(p[2].trim()));
                } catch (NumberFormatException ignored) { }
            }
        } catch (IOException e) {
            System.out.println("Could not read " + SCORE_FILE + ": " + e.getMessage());
        }
    }

    static void saveScores() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(SCORE_FILE))) {
            for (int i = 0; i < scoreCount; i++)
                pw.println(scores[i].name + "," + scores[i].score + "," + scores[i].total);
        } catch (IOException e) {
            System.out.println("Could not save scores: " + e.getMessage());
        }
    }

    // ---------- Input helper ----------
    static String readLine() {
        return sc.hasNextLine() ? sc.nextLine().trim() : null;
    }

    static boolean isValidAnswer(String s) {
        if (s == null || s.length() != 1) return false;
        char c = Character.toUpperCase(s.charAt(0));
        return c >= 'A' && c <= 'D';
    }

    // ---------- Feature 1: Start quiz ----------
    static void startQuiz() {
        if (questionCount == 0) {
            System.out.println("No questions available.");
            return;
        }
        System.out.print("Enter player name: ");
        String name = readLine();
        if (name == null || name.isEmpty()) name = "Player";

        // load all questions into the queue
        QuestionQueue queue = new QuestionQueue(questionCount);
        for (int i = 0; i < questionCount; i++) queue.enqueue(questionBank[i]);
        answerStack.clear();

        int score = 0, qNo = 0;
        while (!queue.isEmpty()) {
            Question q = queue.peek();            // stays in queue until a valid answer arrives
            System.out.println("Q" + (qNo + 1) + ". " + q.text);
            for (int i = 0; i < 4; i++)
                System.out.println("   " + (char) ('A' + i) + ") " + q.options[i]);
            System.out.print("Your answer: ");
            String ans = readLine();
            if (ans == null) return;              // input ended
            if (!isValidAnswer(ans)) {
                System.out.println("Invalid option. Enter A, B, C or D.");
                continue;                         // same question is asked again, not counted
            }
            char given = Character.toUpperCase(ans.charAt(0));
            queue.dequeue();
            qNo++;
            if (given == q.answer) {
                System.out.println("Correct!");
                score++;
            } else {
                System.out.println("Wrong! Correct answer: " + q.answer);
            }
            answerStack.push(new AnswerRecord(qNo, given, q.answer));
        }

        System.out.println("Quiz over. " + name + " scored " + score + "/" + questionCount + ".");
        scores[scoreCount++] = new ScoreEntry(name, score, questionCount);
    }

    // ---------- Feature 2: Review answers (latest first) ----------
    static void reviewAnswers() {
        if (answerStack.isEmpty()) {
            System.out.println("No answers to review. Take a quiz first.");
            return;
        }
        AnswerRecord[] list = answerStack.topToBottom();
        for (AnswerRecord r : list)
            System.out.println("Q" + r.qNo + " | Your answer: " + r.given + " | Correct: " + r.correct);
    }

    // ---------- Sorting: selection sort, highest percentage first ----------
    static boolean isBetter(ScoreEntry a, ScoreEntry b) {
        long left = (long) a.score * b.total;     // compare a.score/a.total with b.score/b.total
        long right = (long) b.score * a.total;
        if (left != right) return left > right;
        return a.score > b.score;
    }

    static ScoreEntry[] sortedScores() {
        ScoreEntry[] arr = new ScoreEntry[scoreCount];
        for (int i = 0; i < scoreCount; i++) arr[i] = scores[i];
        for (int i = 0; i < arr.length - 1; i++) {
            int best = i;
            for (int j = i + 1; j < arr.length; j++)
                if (isBetter(arr[j], arr[best])) best = j;
            ScoreEntry t = arr[i]; arr[i] = arr[best]; arr[best] = t;
        }
        return arr;
    }

    // ---------- Feature 3: Leaderboard ----------
    static void showLeaderboard() {
        if (scoreCount == 0) {
            System.out.println("Leaderboard is empty.");
            return;
        }
        ScoreEntry[] sorted = sortedScores();
        for (int i = 0; i < sorted.length; i++)
            System.out.println((i + 1) + ". " + sorted[i].name + " - "
                    + sorted[i].score + "/" + sorted[i].total);
    }

    // ---------- Menu ----------
    static void printMenu() {
        System.out.println();
        System.out.println("===== ONLINE QUIZ SYSTEM =====");
        System.out.println("1. Start quiz");
        System.out.println("2. Review answers");
        System.out.println("3. Leaderboard");
        System.out.println("4. Exit");
        System.out.print("Choose option: ");
    }

    public static void main(String[] args) {
        sc = new Scanner(System.in);
        loadQuestions();
        loadScores();

        while (true) {
            printMenu();
            String choice = readLine();
            if (choice == null) break;
            switch (choice) {
                case "1": startQuiz(); break;
                case "2": reviewAnswers(); break;
                case "3": showLeaderboard(); break;
                case "4":
                    saveScores();
                    System.out.println("Scores saved. Goodbye!");
                    return;
                default: System.out.println("Invalid menu option. Choose 1-4.");
            }
        }
        saveScores();
    }
}