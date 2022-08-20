package jp.techacademy.yuya.tajima.taskapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_main.*
import android.util.Log
import io.realm.RealmChangeListener
import io.realm.Sort
import android.content.Intent
import androidx.appcompat.app.AlertDialog
import android.app.AlarmManager
import android.app.PendingIntent
import android.widget.SearchView
import io.realm.RealmResults

const val EXTRA_TASK = "jp.techacademy.yuya.tajima.taskapp.TASK"

class MainActivity : AppCompatActivity() {

    private lateinit var mRealm: Realm
    private val mRealmListener = RealmChangeListener<Realm> { reloadListView() }

    private lateinit var mTaskAdapter: TaskAdapter

    private lateinit var taskResults: RealmResults<Task>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fab.setOnClickListener { _ ->
            Log.d("PRINT_DEBUG", "clicked add button")
            val intent = Intent(this, InputActivity::class.java)
            startActivity(intent)
        }

        // Realmの設定
        mRealm = Realm.getDefaultInstance()
        mRealm.addChangeListener(mRealmListener)

        // Realmデータベースから、すべてのデータを取得する
        taskResults = mRealm.where(Task::class.java).findAll()

        // ListViewの設定
        mTaskAdapter = TaskAdapter(this)

        // ListViewをタップしたときの処理
        listView1.setOnItemClickListener { parent, _, position, _ ->
            // 入力・編集する画面に遷移させる
            val task = parent.adapter.getItem(position) as Task
            val intent = Intent(this, InputActivity::class.java)
            intent.putExtra(EXTRA_TASK, task.id)
            startActivity(intent)
            Log.d("PRINT_DEBUG", "clicked a list element")
        }

        // ListViewを長押ししたときの処理
        listView1.setOnItemLongClickListener { parent, _, position, _ ->

            Log.d("PRINT_DEBUG", "long clicked")
            // タスクを削除する
            val task = parent.adapter.getItem(position) as Task

            // ダイアログを表示する
            val builder = AlertDialog.Builder(this)

            builder.setTitle("削除")
            builder.setMessage(task.title + "を削除しますか")

            builder.setPositiveButton("OK"){_, _ ->
                val results = mRealm.where(Task::class.java).equalTo("id", task.id).findAll()

                mRealm.beginTransaction()
                results.deleteAllFromRealm()
                mRealm.commitTransaction()

                val resultIntent = Intent(applicationContext, TaskAlarmReceiver::class.java)
                val resultPendingIntent = PendingIntent.getBroadcast(
                    this,
                    task.id,
                    resultIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT
                )

                val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
                alarmManager.cancel(resultPendingIntent)

                reloadListView()
            }

            builder.setNegativeButton("CANCEL", null)

            val dialog = builder.create()
            dialog.show()

            true
        }

        reloadListView()

        search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {

            override fun onQueryTextChange(text: String): Boolean {
                Log.d("PRINT_DEBUG", "input $text")
                val trimmedString = text.trim()
                if (!trimmedString.isNullOrBlank()) {
                    Log.d("PRINT_DEBUG", "Not empty and blank")
                    taskResults = mRealm.where(Task::class.java).beginsWith("category", trimmedString).findAll()
                } else {
                    taskResults = mRealm.where(Task::class.java).findAll()
                }
                reloadListView()
                return false
            }

            override fun onQueryTextSubmit(query: String): Boolean {
                Log.d("PRINT_DEBUG", "submit!")
                return false
            }

        })

    }

    private fun getAllTask (): RealmResults<Task> {
        // Realmデータベースから、「すべてのデータを取得して新しい日時順に並べた結果」を取得
        return mRealm.where(Task::class.java).findAll()
    }

    private fun reloadListView() {
        // 新しい日時順に並べる
        val taskRealmResults = taskResults.sort("date", Sort.DESCENDING)

        // 上記の結果を、TaskListとしてセットする
        mTaskAdapter.mTaskList = mRealm.copyFromRealm(taskRealmResults)

        // TaskのListView用のアダプタに渡す
        listView1.adapter = mTaskAdapter

        // 表示を更新するために、アダプターにデータが変更されたことを知らせる
        mTaskAdapter.notifyDataSetChanged()
    }

    override fun onDestroy() {
        super.onDestroy()

        mRealm.close()
    }
}