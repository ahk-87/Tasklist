package tasklist

import kotlinx.datetime.*
import com.squareup.moshi.*
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File

const val FILE_JSON = "tasklist.json"

enum class Priority(val color: String) {
    C("\u001B[101m \u001B[0m"), // Critical
    H("\u001B[103m \u001B[0m"), // High
    N("\u001B[102m \u001B[0m"), // Normal
    L("\u001B[104m \u001B[0m")  // Low
}

enum class DueTag(val color: String) {
    T("\u001B[103m \u001B[0m"), // Today
    I("\u001B[102m \u001B[0m"), //
    O("\u001B[101m \u001B[0m")  //
}

// class that handle one task at a specific instance of time, and may contain many actions
class Task(
    val taskLines: MutableList<String> = mutableListOf(),
    var date: String = "",
    var time: String = "",
    var priority: Priority = Priority.C,
    var due: DueTag = DueTag.I
) {

    fun setPriority() {
        while (true) {
            println("Input the task priority (C, H, N, L):")
            val inp = readln().uppercase()
            try {
                priority = Priority.valueOf(inp)
            } catch (e: Exception) {
                continue
            }
            break
        }
    }

    fun setDate() {
        while (true) {
            println("Input the date (yyyy-mm-dd):")
            val inp = readln()
            try {
                val (y, m, d) = inp.split("-").map { it.toInt() }
                val dt = LocalDate(y, m, d)
                date = dt.toString()
            } catch (e: Exception) {
                println("The input date is invalid")
                continue
            }
            updateDue()
            break
        }
    }

    fun setTime() {
        while (true) {
            println("Input the time (hh:mm):")
            val inp = readln()
            try {
                val (h, m) = inp.split(":").map { it.toInt() }
                val dt = LocalDateTime(2010, 10, 10, h, m)
                time = dt.toString().split("T")[1]
            } catch (e: Exception) {
                println("The input time is invalid")
                continue
            }
            break
        }
    }

    fun setActions(): Boolean {
        println("Input a new task (enter a blank line to end):")
        while (true) {
            val inp = readln()
            if (inp.isBlank()) {
                return taskLines.isNotEmpty()
            } else {
                taskLines.add(inp)
            }
        }
    }

    fun edit() {
        while (true) {
            println("Input a field to edit (priority, date, time, task):")
            when (readln().lowercase()) {
                "priority" -> setPriority()
                "date" -> setDate()
                "time" -> setTime()
                "task" -> {
                    taskLines.clear(); setActions()
                }

                else -> {
                    println("Invalid field"); continue
                }
            }
            println("The task is changed")
            break
        }
    }

    private fun updateDue() {
        val taskDate = LocalDate.parse(date)
        val currentDate = Clock.System.now().toLocalDateTime(TimeZone.of("UTC+0")).date
        val numberOfDays = currentDate.daysUntil(taskDate)
        due = when {
            numberOfDays == 0 -> DueTag.T
            numberOfDays > 0 -> DueTag.I
            else -> DueTag.O
        }
    }
}

// a container that contains a list of tasks, and manage the adding, editing, deleting and printing of tasks
class TaskClass {
    val tasks = mutableListOf<Task>()

    fun addTask() {
        val t = Task()
        t.setPriority()
        t.setDate()
        t.setTime()
        if (t.setActions()) tasks.add(t) else println("The task is blank")
    }

    fun editTask() {
        if (tasks.isEmpty()) {
            println("No tasks have been input"); return
        }

        printTasks()
        while (true) {
            println("Input the task number (1-${tasks.size}):")
            val taskIndexToEdit = readln().toIntOrNull()
            if (taskIndexToEdit == null || taskIndexToEdit !in 1..tasks.size) {
                println("Invalid task number")
            } else {
                tasks[taskIndexToEdit - 1].edit()
                break
            }
        }
    }

    fun deleteTask() {
        if (tasks.isEmpty()) {
            println("No tasks have been input"); return
        }

        printTasks()
        while (true) {
            println("Input the task number (1-${tasks.size}):")
            val taskIndexToDelete = readln().toIntOrNull()
            if (taskIndexToDelete == null || taskIndexToDelete !in 1..tasks.size) {
                println("Invalid task number")
            } else {
                tasks.removeAt(taskIndexToDelete - 1)
                println("The task is deleted")
                break
            }
        }
    }

    fun printTasks() {
        if (tasks.isEmpty()) {
            println("No tasks have been input"); return
        }

        val separatorLine = "+----+------------+-------+---+---+--------------------------------------------+"
        val lf = "| %-2s | %-10s | %-5s | %s | %s |%-44s|"

        println(separatorLine)
        println("| N  |    Date    | Time  | P | D |                   Task                     |")
        println(separatorLine)

        for (i in tasks.indices) {
            val t = tasks[i]
            for (j in t.taskLines.indices) {
                val chunks = t.taskLines[j].chunked(44)
                var usedChunk = 0

                if (j == 0) {
                    println(lf.format(i + 1, t.date, t.time, t.priority.color, t.due.color, chunks[0]))
                    usedChunk++
                }

                for (nextChunk in usedChunk..chunks.lastIndex) {
                    println(lf.format("", "", "", " ", " ", chunks[nextChunk]))
                }
            }
            println(separatorLine)
        }
    }
}

// functions that manage file handling of the application (saving and loading data in json format)
fun getData(): List<Task>? {
    val file = File(FILE_JSON)
    if (file.exists()) {
        val content = file.readText()
        val jsonAdapter = getAdapter()
        return jsonAdapter.fromJson(content)?.toList()
    }
    return null
}

fun saveData(data: TaskClass) {
    val file = File(FILE_JSON)
    val jsonAdapter = getAdapter()
    file.writeText(jsonAdapter.toJson(data.tasks))
}

fun getAdapter(): JsonAdapter<List<Task>>{
    val moshi: Moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    val type = Types.newParameterizedType(MutableList::class.java, Task::class.java)
    return moshi.adapter<List<Task>>(type)
}

fun main() {
    val tasksOrganizer = TaskClass()
    val lists = getData()
    if (lists != null) tasksOrganizer.tasks.addAll(lists)

    while (true) {
        println("Input an action (add, print, edit, delete, end):")
        val input = readln().lowercase()
        when (input) {
            "add" -> tasksOrganizer.addTask()
            "print" -> tasksOrganizer.printTasks()
            "edit" -> tasksOrganizer.editTask()
            "delete" -> tasksOrganizer.deleteTask()
            "end" -> {
                saveData(tasksOrganizer)
                println("Tasklist exiting!")
                break
            }

            else -> println("The input action is invalid")
        }
    }
}