import com.sun.net.httpserver.HttpServer; // 小さなWebサーバーを作る機能を読み込みます
import java.io.IOException; // 入出力時のエラーを扱う機能を読み込みます
import java.io.OutputStream; // ブラウザへデータを送る機能を読み込みます
import java.net.InetSocketAddress; // 待ち受けるポート番号を指定する機能を読み込みます
import java.net.URLDecoder; // ：URL用に変換された文字を元に戻す機能を読み込みます
import java.nio.charset.StandardCharsets; // UTF-8を指定する機能を読み込みます
import java.nio.file.Files; // ★追加 ファイルを読み書きする機能を読み込みます
import java.nio.file.Path; // ★追加 ファイルの場所を表す機能を読み込みます
import java.time.LocalDate; // 締切日を扱う機能を読み込みます
import java.time.YearMonth; // カレンダーの年月を扱う機能を読み込みます
import java.util.ArrayList; // ：あとからTodoを追加できるListを作る機能を読み込みます
import java.util.Base64; // タイトルを安全にファイルへ保存する機能を読み込みます
import java.util.Comparator; // Todoを指定した順番に並べ替える機能を読み込みます
import java.util.List; // ：複数のTodoを順番に入れるListを使えるようにします

class Todo { // ★変更 Todo（1件分のやること）の設計図です
    private int id; // ★変更 何番のTodoかを保存します
    private String title; // ★変更 やることを保存します
    private boolean done; // ★変更 終わったかを保存します
    private String deadline; // 締切日を「年-月-日」の形で保存します
    private long updatedAt; // 最後に状態を更新した時刻を保存します

    public Todo(int id, String title, boolean done) { // ★変更 Todoを1件作るときに値を受け取ります
        this(id, title, done, "", System.currentTimeMillis()); // 締切日なし・現在時刻でTodoを作ります
    } // 従来の作り方を保つコンストラクターの終わりです

    public Todo(int id, String title, boolean done, String deadline, long updatedAt) { // 保存済みの全項目からTodoを作ります
        this.id = id; // ★変更 受け取った番号を保存します
        this.title = title; // ★変更 受け取ったやることを保存します
        this.done = done; // ★変更 受け取った終了状態を保存します
        this.deadline = deadline; // 受け取った締切日を保存します
        this.updatedAt = updatedAt; // 受け取った更新時刻を保存します
    } // ★変更 Todoを作る処理の終わりです

    public int getId() { // ★変更 idを読み出すメソッド（処理）です
        return id; // ★変更 idを返します
    } // ★変更 getIdの終わりです

    public String getTitle() { // ★変更 titleを読み出すメソッド（処理）です
        return title; // ★変更 titleを返します
    } // ★変更 getTitleの終わりです

    public boolean isDone() { // ★変更 doneを読み出すメソッド（処理）です
        return done; // ★変更 doneを返します
    } // ★変更 isDoneの終わりです

    public void setDone(boolean done) { // ★変更 doneを書き換えるメソッド（処理）です
        this.done = done; // ★変更 doneを新しい値にします
        this.updatedAt = System.currentTimeMillis(); // 状態を変えた時刻を更新します
    } // ★変更 setDoneの終わりです

    public String getDeadline() { // 締切日を読み出すメソッドです
        return deadline; // 締切日を返します
    } // getDeadlineの終わりです

    public long getUpdatedAt() { // 更新時刻を読み出すメソッドです
        return updatedAt; // 更新時刻を返します
    } // getUpdatedAtの終わりです
} // ★変更 Todoクラスの終わりです

public class App { // Appという名前のプログラムを定義します
    static List<Todo> todos = new ArrayList<>(); // ★変更 Todoを貯めるList（順番に保存する箱）です
    static int nextId = 1; // ★変更 次に使う1からの番号です

    static void save() throws IOException { // ★追加 現在のTodo全件をtodos.csvへ保存します
        List<String> lines = new ArrayList<>(); // ★追加 CSVへ書く行を入れるListを用意します
        for (Todo todo : todos) { // ★追加 Todoを1件ずつ取り出します
            String encodedTitle = Base64.getUrlEncoder().withoutPadding().encodeToString( // タイトルを改行やカンマに影響されない形へ変換します
                    todo.getTitle().getBytes(StandardCharsets.UTF_8)); // 日本語をUTF-8で変換します
            lines.add("v2," + todo.getId() + "," + (todo.isDone() ? "1" : "0") + "," // 新形式の識別子、id、完了状態を並べます
                    + todo.getUpdatedAt() + "," + todo.getDeadline() + "," + encodedTitle); // 更新時刻、締切日、タイトルを1行にします
        } // ★追加 すべてのTodoをCSVの行にする処理を終えます
        Files.write(Path.of("todos.csv"), lines, StandardCharsets.UTF_8); // ★追加
        // 全行をUTF-8でtodos.csvへ書き出します
    } // ★追加 saveメソッドの終わりです

    static void load() throws IOException { // ★追加 todos.csvからTodo全件を読み込みます
        Path path = Path.of("todos.csv"); // ★追加 読み込むファイルの場所を用意します
        todos.clear(); // ★追加 読み込み前にListを空にします
        nextId = 1; // ★追加 ファイルがない場合に備えて次の番号を1へ戻します
        if (!Files.exists(path)) { // ★追加 todos.csvがあるかを調べます
            return; // ★追加 ファイルがなければ空のまま読み込みを終えます
        } // ★追加 ファイルの有無の確認を終えます
        int maxId = 0; // ★追加 読み込んだ中で最も大きいidを覚える箱を用意します
        for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) { // ★追加 UTF-8で各行を読み込みます
            try { // 壊れた行があってもほかのTodoを読み込めるようにします
                int id; // 読み込んだidを入れる箱を用意します
                if (line.startsWith("v2,")) { // 締切日と更新時刻を持つ新形式かを調べます
                    String[] values = line.split(",", 6); // 新形式の6項目に分けます
                    if (values.length != 6) { // 必要な項目がそろっているかを調べます
                        continue; // 項目が不足した行は読み飛ばします
                    } // 新形式の項目数確認を終えます
                    id = Integer.parseInt(values[1]); // idを数字に変換します
                    boolean done = values[2].equals("1"); // 完了状態を読み取ります
                    long updatedAt = Long.parseLong(values[3]); // 更新時刻を読み取ります
                    String title = new String(Base64.getUrlDecoder().decode(values[5]), StandardCharsets.UTF_8); // 日本語タイトルを元に戻します
                    todos.add(new Todo(id, title, done, values[4], updatedAt)); // 新形式のTodoをListへ戻します
                } else { // 従来の保存形式の場合です
                    String[] values = line.split(",", 3); // 従来形式をid、完了状態、タイトルに分けます
                    if (values.length != 3) { // 必要な項目がそろっているかを調べます
                        continue; // 項目が不足した行は読み飛ばします
                    } // 従来形式の項目数確認を終えます
                    id = Integer.parseInt(values[0]); // 従来形式のidを数字に変換します
                    boolean done = values[1].equals("1"); // 従来形式の完了状態を読み取ります
                    todos.add(new Todo(id, values[2], done, "", id)); // 従来データを締切日なしでListへ戻します
                } // 保存形式ごとの読み込みを終えます
                maxId = Math.max(maxId, id); // ★追加 最も大きいidを更新します
            } catch (IllegalArgumentException e) { // 数字やタイトルの形式が壊れた行を受け止めます
                System.err.println("読み込めないTodoをスキップしました: " + line); // 読み飛ばした行を確認できるようにします
            } // 1行の読み込み処理を終えます
        } // ★追加 CSVの全行を読み込む処理を終えます
        nextId = maxId + 1; // ★追加 次の番号を最も大きいidの次にします
    } // ★追加 loadメソッドの終わりです

    static String todosToJson() { // ★追加 全TodoをJSON配列へ変換します
        StringBuilder json = new StringBuilder("["); // ★追加 JSON配列の開始記号を用意します
        for (int i = 0; i < todos.size(); i++) { // ★追加 Todoを先頭から順番に取り出します
            if (i > 0) { // ★追加 2件目以降かを調べます
                json.append(","); // ★追加 Todo同士を区切るカンマを追加します
            } // ★追加 カンマが必要かの確認を終えます
            Todo todo = todos.get(i); // ★追加 現在のTodoを取り出します
            json.append("{\"title\":\""); // ★追加 Todoオブジェクトとタイトル項目を開始します
            json.append(escapeJson(todo.getTitle())); // ★追加 タイトルを安全なJSON文字列として追加します
            json.append("\",\"done\":"); // ★追加 タイトルを閉じて完了状態の項目を開始します
            json.append(todo.isDone()); // ★追加 完了状態をtrueまたはfalseで追加します
            json.append("}"); // ★追加 Todoオブジェクトを閉じます
        } // ★追加 全Todoの変換を終えます
        json.append("]"); // ★追加 JSON配列を閉じます
        return json.toString(); // ★追加 完成したJSON文字列を返します
    } // ★追加 Todo一覧のJSON変換を終えます

    static String escapeJson(String value) { // ★追加 文字列をJSONで安全に使える形へ変換します
        StringBuilder escaped = new StringBuilder(); // ★追加 変換後の文字列を組み立てる箱を用意します
        for (int i = 0; i < value.length(); i++) { // ★追加 元の文字列を1文字ずつ調べます
            char c = value.charAt(i); // ★追加 現在の文字を取り出します
            switch (c) { // ★追加 JSONで特別な扱いが必要な文字を判定します
                case '"': // ★追加 ダブルクォートの場合です
                    escaped.append("\\\""); // ★追加 ダブルクォートをエスケープして追加します
                    break; // ★追加 この文字の処理を終えます
                case '\\': // ★追加 バックスラッシュの場合です
                    escaped.append("\\\\"); // ★追加 バックスラッシュをエスケープして追加します
                    break; // ★追加 この文字の処理を終えます
                case '\b': // ★追加 バックスペースの場合です
                    escaped.append("\\b"); // ★追加 JSONのバックスペース表記を追加します
                    break; // ★追加 この文字の処理を終えます
                case '\f': // ★追加 フォームフィードの場合です
                    escaped.append("\\f"); // ★追加 JSONのフォームフィード表記を追加します
                    break; // ★追加 この文字の処理を終えます
                case '\n': // ★追加 改行の場合です
                    escaped.append("\\n"); // ★追加 JSONの改行表記を追加します
                    break; // ★追加 この文字の処理を終えます
                case '\r': // ★追加 復帰の場合です
                    escaped.append("\\r"); // ★追加 JSONの復帰表記を追加します
                    break; // ★追加 この文字の処理を終えます
                case '\t': // ★追加 タブの場合です
                    escaped.append("\\t"); // ★追加 JSONのタブ表記を追加します
                    break; // ★追加 この文字の処理を終えます
                default: // ★追加 そのほかの文字の場合です
                    if (c < 0x20) { // ★追加 JSONへ直接書けない制御文字かを調べます
                        escaped.append(String.format("\\u%04x", (int) c)); // ★追加 制御文字をUnicode表記で追加します
                    } else { // ★追加 通常の文字の場合です
                        escaped.append(c); // ★追加 元の文字をそのまま追加します
                    } // ★追加 制御文字かの確認を終えます
            } // ★追加 現在の文字の判定を終えます
        } // ★追加 全文字の変換を終えます
        return escaped.toString(); // ★追加 エスケープ済みの文字列を返します
    } // ★追加 JSON文字列のエスケープ処理を終えます

    static String getParameter(String source, String name) { // フォームやURLから指定した項目を取り出します
        if (source == null || source.isBlank()) { // 調べる文字がないかを確認します
            return ""; // 項目がない場合は空文字を返します
        } // 調べる文字の有無の確認を終えます
        for (String parameter : source.split("&")) { // 「&」で区切られた項目を順番に調べます
            String[] pair = parameter.split("=", 2); // 項目名と値の2つに分けます
            try { // 不正なURL表記があっても処理を続けられるようにします
                String key = URLDecoder.decode(pair[0], StandardCharsets.UTF_8); // 項目名を元の文字へ戻します
                if (key.equals(name)) { // 探している項目名かを調べます
                    return pair.length == 2 ? URLDecoder.decode(pair[1], StandardCharsets.UTF_8) : ""; // 値を元の文字へ戻して返します
                } // 項目名の確認を終えます
            } catch (IllegalArgumentException e) { // 不正なURL表記を受け止めます
                return ""; // 不正な項目は空文字として扱います
            } // URL表記の変換を終えます
        } // 全項目を調べる処理を終えます
        return ""; // 指定した項目がなければ空文字を返します
    } // 項目を取り出す処理の終わりです

    static String escapeHtml(String value) { // 文字列をHTMLへ安全に表示できる形へ変換します
        return value.replace("&", "&amp;") // アンパサンドをHTML用に変換します
                .replace("<", "&lt;") // 小なり記号をHTML用に変換します
                .replace(">", "&gt;") // 大なり記号をHTML用に変換します
                .replace("\"", "&quot;") // ダブルクォートをHTML用に変換します
                .replace("'", "&#39;"); // シングルクォートをHTML用に変換します
    } // HTML用の変換処理を終えます

    static String renderCalendar(YearMonth month, String sort) { // 指定した月の締切カレンダーを作ります
        StringBuilder html = new StringBuilder(); // カレンダーのHTMLを組み立てる箱を用意します
        html.append("<section class='panel calendar-panel'><div class='calendar-title'>"); // カレンダー枠と見出しを始めます
        html.append("<a class='month-link' href='/?sort=").append(sort).append("&month=").append(month.minusMonths(1))
                .append("'>←</a>"); // 前月へのリンクを追加します
        html.append("<h2>").append(month.getYear()).append("年 ").append(month.getMonthValue()).append("月の締切</h2>"); // 表示中の年月を追加します
        html.append("<a class='month-link' href='/?sort=").append(sort).append("&month=").append(month.plusMonths(1))
                .append("'>→</a></div>"); // 翌月へのリンクを追加します
        html.append(
                "<table><thead><tr><th>月</th><th>火</th><th>水</th><th>木</th><th>金</th><th>土</th><th>日</th></tr></thead><tbody><tr>"); // 曜日の見出しを追加します
        int column = 0; // 現在の曜日位置を覚える箱を用意します
        int firstOffset = month.atDay(1).getDayOfWeek().getValue() - 1; // 1日より前に必要な空欄数を求めます
        for (; column < firstOffset; column++) { // 月初までの空欄を追加します
            html.append("<td class='empty'></td>"); // 日付のないセルを追加します
        } // 月初までの空欄追加を終えます
        for (int dayNumber = 1; dayNumber <= month.lengthOfMonth(); dayNumber++) { // 月の全日付を順番に表示します
            if (column == 7) { // 1週間分を表示し終えたかを調べます
                html.append("</tr><tr>"); // 次の週の行を開始します
                column = 0; // 曜日位置を月曜日へ戻します
            } // 週の切り替え確認を終えます
            LocalDate day = month.atDay(dayNumber); // 現在の日付を作ります
            String todayClass = day.equals(LocalDate.now()) ? " today" : ""; // 今日だけに付ける見た目を決めます
            html.append("<td class='day").append(todayClass).append("'><span class='day-number'>").append(dayNumber)
                    .append("</span>"); // 日付セルを開始します
            for (Todo todo : todos) { // 現在の日が締切のTodoを探します
                if (todo.getDeadline().equals(day.toString())) { // 締切日が現在の日と一致するかを調べます
                    html.append("<div class='calendar-task").append(todo.isDone() ? " done" : "").append("'>"); // カレンダー内のTodo表示を開始します
                    html.append(escapeHtml(todo.getTitle())).append("</div>"); // 安全に変換したタイトルを追加します
                } // 締切日の確認を終えます
            } // 現在の日のTodoを探す処理を終えます
            html.append("</td>"); // 日付セルを閉じます
            column++; // 次の曜日位置へ進めます
        } // 月の全日付の表示を終えます
        while (column > 0 && column < 7) { // 月末から日曜日までの空欄を追加します
            html.append("<td class='empty'></td>"); // 日付のないセルを追加します
            column++; // 次の曜日位置へ進めます
        } // 月末の空欄追加を終えます
        html.append("</tr></tbody></table></section>"); // カレンダー表と枠を閉じます
        return html.toString(); // 完成したカレンダーHTMLを返します
    } // カレンダー作成処理の終わりです

    static String renderPage(String sort, YearMonth month) { // Todo画面全体のHTMLを作ります
        long completedCount = todos.stream().filter(Todo::isDone).count(); // 完了済みTodoの件数を数えます
        long activeCount = todos.size() - completedCount; // 現在取り組むTodoの件数を求めます
        List<Todo> displayedTodos = new ArrayList<>(todos); // 保存順を変えないよう表示用Listを作ります
        if (sort.equals("name")) { // 名前順が選ばれているかを調べます
            displayedTodos.sort(
                    Comparator.comparing(Todo::getTitle, String.CASE_INSENSITIVE_ORDER).thenComparingInt(Todo::getId)); // タイトル、idの順に並べます
        } else { // 更新順が選ばれている場合です
            displayedTodos.sort(Comparator.comparingLong(Todo::getUpdatedAt).reversed()
                    .thenComparing(Comparator.comparingInt(Todo::getId).reversed())); // 新しく更新したTodoから並べます
        } // 並べ替え方法の選択を終えます
        StringBuilder html = new StringBuilder(); // 画面全体のHTMLを組み立てる箱を用意します
        html.append(
                "<!doctype html><html lang='ja'><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width,initial-scale=1'>"); // 日本語とスマートフォン表示の設定を追加します
        html.append("<title>わたしのTodo</title><style>"); // ページタイトルとCSSを開始します
        html.append(
                "*{box-sizing:border-box}body{margin:0;background:#fff8ef;color:#4b342b;font-family:'Yu Gothic',sans-serif}main{max-width:1100px;margin:auto;padding:32px 18px 48px}h1{margin:0;color:#a84f32}h2{margin:0;font-size:1.15rem}.lead{color:#8a6557;margin:6px 0 24px}.summary{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:14px;margin-bottom:18px}.count-card,.panel{background:#fffdf9;border:1px solid #efd8c5;border-radius:16px;box-shadow:0 8px 24px #a84f3212}.count-card{padding:18px}.count-card strong{display:block;font-size:2rem;color:#c65f3c}.workspace{display:grid;grid-template-columns:minmax(0,1fr) minmax(360px,1.15fr);gap:18px}.panel{padding:20px}.add-form{display:grid;grid-template-columns:1fr auto auto;gap:10px;margin:16px 0}.add-form input,.sort-form select{border:1px solid #dcbba5;border-radius:10px;padding:11px;background:white;color:#4b342b}button,.action,.month-link{border:0;border-radius:10px;background:#d96f45;color:white;padding:10px 14px;text-decoration:none;cursor:pointer;font-weight:bold}.sort-form{display:flex;align-items:center;gap:8px;margin-bottom:12px}.todo-list{list-style:none;margin:0;padding:0}.todo-item{display:flex;justify-content:space-between;gap:12px;align-items:center;border-top:1px solid #f2dfd1;padding:14px 2px}.todo-item.done .todo-title{text-decoration:line-through;color:#9b877f}.todo-main{min-width:0}.todo-title{font-weight:bold;overflow-wrap:anywhere}.deadline{display:block;color:#a6654d;font-size:.85rem;margin-top:4px}.actions{display:flex;gap:6px;flex-shrink:0}.action{font-size:.85rem;padding:7px 9px}.action.secondary{background:#bf9278}.action.delete{background:#8f5d50}.empty-message{padding:22px;text-align:center;color:#9b7565}.calendar-title{display:flex;align-items:center;justify-content:space-between;margin-bottom:14px}.month-link{padding:7px 11px}table{width:100%;border-collapse:separate;border-spacing:4px;table-layout:fixed}th{color:#a45c43;font-size:.8rem}td{vertical-align:top}.day{height:88px;background:#fff7ed;border-radius:9px;padding:6px}.day.today{outline:2px solid #e1845e}.day-number{font-size:.82rem;font-weight:bold}.calendar-task{margin-top:4px;padding:3px 5px;border-radius:5px;background:#f4b38f;color:#633526;font-size:.72rem;overflow:hidden;text-overflow:ellipsis}.calendar-task.done{text-decoration:line-through;background:#dcc3b4}.empty{background:#fbf0e5;border-radius:9px}@media(max-width:820px){.workspace{grid-template-columns:1fr}.add-form{grid-template-columns:1fr}.calendar-panel{overflow-x:auto}table{min-width:560px}.summary{grid-template-columns:1fr 1fr}}@media(max-width:480px){.summary{grid-template-columns:1fr}.todo-item{align-items:flex-start;flex-direction:column}} "); // 暖色系のレイアウトと画面幅に応じた見た目を指定します
        html.append(
                "</style></head><body><main><header><h1>わたしのTodo</h1><p class='lead'>今日の予定と締切を、ひと目で確認できます。</p></header>"); // CSSを閉じて画面の見出しを追加します
        html.append("<section class='summary'><div class='count-card'><span>現在のタスク</span><strong>").append(activeCount)
                .append("</strong></div>"); // 現在のタスク件数を表示します
        html.append("<div class='count-card'><span>完了したタスク</span><strong>").append(completedCount)
                .append("</strong></div></section>"); // 完了件数を表示します
        html.append("<div class='workspace'><section class='panel'><h2>Todo一覧</h2>"); // Todo一覧の枠を開始します
        html.append(
                "<form class='add-form' method='post' action='/add' accept-charset='UTF-8'><input name='todo' placeholder='新しいTodo' required><input type='date' name='deadline'><button type='submit'>追加</button></form>"); // タイトルと締切日を入力するフォームを追加します
        html.append(
                "<form class='sort-form' method='get' action='/'><label for='sort'>並べ替え</label><select id='sort' name='sort'>"); // 並べ替えフォームを開始します
        html.append("<option value='updated'").append(sort.equals("updated") ? " selected" : "")
                .append(">更新順</option>"); // 更新順の選択肢を追加します
        html.append("<option value='name'").append(sort.equals("name") ? " selected" : "")
                .append(">名前順</option></select>"); // 名前順の選択肢を追加します
        html.append("<input type='hidden' name='month' value='").append(month)
                .append("'><button type='submit'>表示</button></form>"); // 表示中の月を保って並べ替えるボタンを追加します
        html.append("<ul class='todo-list'>"); // Todo一覧を開始します
        if (displayedTodos.isEmpty()) { // Todoが1件もないかを調べます
            html.append("<li class='empty-message'>Todoはまだありません。</li>"); // 空の一覧に案内を表示します
        } // 空の一覧かの確認を終えます
        for (Todo todo : displayedTodos) { // 表示順にTodoを取り出します
            html.append("<li class='todo-item").append(todo.isDone() ? " done" : "")
                    .append("'><div class='todo-main'><span class='todo-title'>"); // Todo項目とタイトルを開始します
            html.append(escapeHtml(todo.getTitle())).append("</span>"); // 日本語タイトルを安全に表示します
            if (!todo.getDeadline().isBlank()) { // 締切日が設定されているかを調べます
                html.append("<span class='deadline'>締切: ").append(escapeHtml(todo.getDeadline())).append("</span>"); // 締切日を表示します
            } // 締切日の表示を終えます
            html.append("</div><div class='actions'>"); // タイトル部分を閉じて操作部分を始めます
            if (todo.isDone()) { // 完了済みかを調べます
                html.append("<a class='action secondary' href='/undo?id=").append(todo.getId()).append("'>取り消し</a>"); // 完了を取り消すリンクを表示します
            } else { // 未完了の場合です
                html.append("<a class='action' href='/done?id=").append(todo.getId()).append("'>完了</a>"); // 完了にするリンクを表示します
            } // 完了状態ごとの操作表示を終えます
            html.append("<a class='action delete' href='/delete?id=").append(todo.getId())
                    .append("'>削除</a></div></li>"); // 削除リンクを追加してTodo項目を閉じます
        } // 全Todoの表示を終えます
        html.append("</ul></section>"); // Todo一覧と枠を閉じます
        html.append(renderCalendar(month, sort)); // 別枠の締切カレンダーを追加します
        html.append("</div></main></body></html>"); // 画面全体を閉じます
        return html.toString(); // 完成したHTMLを返します
    } // Todo画面の作成処理を終えます

    public static void main(String[] args) throws IOException { // プログラムの開始地点です
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0); // 8080番ポートでWebサーバーを作ります
        load(); // ★追加 起動時にtodos.csvがあればTodo全件を読み込みます

        server.createContext("/api/todos", exchange -> { // ★追加 Todo一覧APIへのアクセスを受け取ります
            if (!exchange.getRequestMethod().equals("GET")) { // ★追加 GET以外のリクエストかを調べます
                exchange.getResponseHeaders().set("Allow", "GET"); // ★追加 利用できるメソッドがGETであることを伝えます
                exchange.sendResponseHeaders(405, -1); // ★追加 利用できないメソッドとして405を返します
                exchange.close(); // ★追加 応答を閉じます
                return; // ★追加 APIの処理を終了します
            } // ★追加 メソッドの確認を終えます
            String json = todosToJson(); // ★追加 全TodoをJSON文字列に変換します
            byte[] response = json.getBytes(StandardCharsets.UTF_8); // ★追加 JSONをUTF-8のデータに変換します
            exchange.getResponseHeaders().set("Content-Type", "application/json"); // ★追加 charsetなしのJSON形式を指定します
            exchange.sendResponseHeaders(200, response.length); // ★追加 正常終了とデータの長さを返します
            try (OutputStream output = exchange.getResponseBody()) { // ★追加 JSONを返すための出力先を開きます
                output.write(response); // ★追加 JSONデータをクライアントへ送ります
            } // ★追加 出力先を自動的に閉じます
        }); // ★追加 Todo一覧APIの登録を終えます

        server.createContext("/", exchange -> { // 「/」へのアクセスを受け取ったときの処理です
            String path = exchange.getRequestURI().getPath(); // アクセスされたパスを取り出します
            String message;

            // パスを if / else if / else で比べます
            if (path.equals("/add") && exchange.getRequestMethod().equals("POST")) { // ：フォームから「/add」へ送られたPOSTかを調べます
                String formData = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8); // ：送られたフォームの内容をUTF-8で読み取ります
                String todo = getParameter(formData, "todo"); // 日本語を含むTodoタイトルを取り出します
                String deadline = getParameter(formData, "deadline"); // 入力された締切日を取り出します
                if (!deadline.isBlank()) { // 締切日が入力されているかを調べます
                    try { // 締切日が正しい日付かを確認します
                        LocalDate.parse(deadline); // 年月日の形式として解析します
                    } catch (RuntimeException e) { // 正しくない日付を受け止めます
                        deadline = ""; // 正しくない締切日は未設定として扱います
                    } // 締切日の確認を終えます
                } // 締切日の入力有無の確認を終えます
                if (!todo.isBlank()) { // ：空の入力ではないかを調べます
                    todos.add(new Todo(nextId++, todo, false, deadline, System.currentTimeMillis())); // 番号、締切日、更新時刻を付けて未完了Todoを追加します
                    save(); // ★追加 Todoを追加した直後の全件をtodos.csvへ保存します
                } // ：空の入力を追加しないための確認を終えます
                exchange.getResponseHeaders().set("Location", "/"); // ：追加後の移動先が「/」だとブラウザへ伝えます
                exchange.sendResponseHeaders(303, -1); // ：303で「/」へ移動するようブラウザへ伝えます
                exchange.close(); // ：この応答を閉じます
                return; // ：通常のページを返す処理へ進まないよう、ここで終えます
            } else if ((path.equals("/done") || path.equals("/undo") || path.equals("/delete"))
                    && exchange.getRequestMethod().equals("GET")) { // 完了、取り消し、削除のGETアクセスを処理します
                String query = exchange.getRequestURI().getRawQuery(); // ★追加 URLの「?」より後ろを取り出します
                Integer targetId = null; // ★追加 操作するTodoのidを入れる箱を用意します
                try { // idを数字へ変換できるかを確認します
                    targetId = Integer.parseInt(getParameter(query, "id")); // URLからidを取り出して数字に変換します
                } catch (NumberFormatException e) { // idがない場合や数字でない場合を受け止めます
                    targetId = null; // 何も変更しない状態にします
                } // idの変換を終えます
                if (targetId != null && (path.equals("/done") || path.equals("/undo"))) { // 完了または取り消しが押された場合です
                    for (Todo todo : todos) { // ★追加 Todoを1件ずつ調べます
                        if (todo.getId() == targetId) { // ★追加 idが一致するTodoを見つけたか調べます
                            todo.setDone(path.equals("/done")); // 完了ならtrue、取り消しならfalseへ変更します
                            save(); // 状態を変更した直後の全件をtodos.csvへ保存します
                            break; // ★追加 1件だけ変更して繰り返しを止めます
                        } // ★追加 idの確認を終えます
                    } // ★追加 Todoを調べる繰り返しを終えます
                } else if (targetId != null && path.equals("/delete")) { // ★追加 正しいidで削除が押された場合です
                    for (int i = 0; i < todos.size(); i++) { // ★追加 Todoの位置を先頭から調べます
                        if (todos.get(i).getId() == targetId) { // ★追加 idが一致するTodoを見つけたか調べます
                            todos.remove(i); // ★追加 ★修正 idが一致した位置のTodoをListから取り除きます
                            save(); // ★追加 Todoを削除した直後の全件をtodos.csvへ保存します
                            break; // ★追加 1件だけ削除して繰り返しを止めます
                        } // ★追加 idの確認を終えます
                    } // ★追加 Todoを調べる繰り返しを終えます
                } // ★追加 完了または削除の処理を終えます
                exchange.getResponseHeaders().set("Location", "/"); // ★追加 移動先が「/」だとブラウザへ伝えます
                exchange.sendResponseHeaders(303, -1); // ★追加 303で「/」へ戻します
                exchange.close(); // ★追加 この応答を閉じます
                return; // ★追加 通常のページを返す処理へ進まず、ここで終えます
            } else if (path.equals("/hello")) {
                String query = exchange.getRequestURI().getRawQuery(); // ：URLの「?」より後ろの部分を取り出します
                System.out.println("query = " + query);
                String name; // 修正：挨拶に使う名前を入れる変数を用意します
                if (query == null) { // 修正：URLに「?name=」がなく、queryに値がないかを調べます
                    name = "ゲスト"; // 修正：値がない場合は「ゲスト」を使います
                } else {
                    String encodedName = query.substring("name=".length()); // ：「name=」より後ろの、変換された名前を切り出します
                    name = URLDecoder.decode(encodedName, StandardCharsets.UTF_8); // ：%ではじまる表記を元の文字に戻します
                }
                message = "こんにちは、" + name + "さん！"; // ：取り出した名前を応答に入れます
            } else if (path.equals("/bye")) {
                message = "さようなら！";
            } else if (path.equals("/good")) {
                message = "元気！";
            } else if (path.equals("/")) { // 変更：「/」へのアクセスかを調べます
                String query = exchange.getRequestURI().getRawQuery(); // 並べ替えと表示月の指定を取り出します
                String sort = getParameter(query, "sort"); // 並べ替え方法を取り出します
                if (!sort.equals("name")) { // 名前順以外が指定されているかを調べます
                    sort = "updated"; // 初期表示と不正な指定は更新順にします
                } // 並べ替え方法の確認を終えます
                YearMonth month; // 表示する年月を入れる箱を用意します
                try { // URLの年月を解析できるかを確認します
                    month = YearMonth.parse(getParameter(query, "month")); // 指定された年月を読み取ります
                } catch (RuntimeException e) { // 年月が未指定または不正な場合を受け止めます
                    month = YearMonth.now(); // デフォルトで今月を表示します
                } // 表示月の決定を終えます
                message = renderPage(sort, month); // 件数、一覧、カレンダーを含む画面を作ります
            } else {
                message = "ページが見つかりません";
            }

            byte[] response = message.getBytes(StandardCharsets.UTF_8); // 文字をUTF-8のデータに変換します
            if (path.equals("/")) { // 変更：「/」の応答かを調べます
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8"); // HTMLをUTF-8で返して日本語の文字化けを防ぎます
            } else { // 変更：「/」以外の場合です
                exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8"); // 今までどおり通常の文字をUTF-8で返します
            } // ：Content-Typeの選び分けを終えます
            exchange.sendResponseHeaders(200, response.length); // 正常終了を表す200とデータの長さを送ります

            try (OutputStream output = exchange.getResponseBody()) { // ブラウザへ送るための通り道を開きます
                output.write(response); // ブラウザへ文字のデータを送ります
            } // 送信の通り道を自動で閉じます
        }); // 「/」への処理の登録を終えます

        server.start(); // Webサーバーの待ち受けを開始します
        System.out.println("サーバー起動: http://localhost:8080 （止めるときは Ctrl+C）"); // 起動したことをターミナルへ表示します
    } // mainの定義を終えます
} // Appの定義を終えます
