package onlymash.flexbooru.ap.ui

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.annotation.IdRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.appbar.AppBarLayout
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar.*
import kotlinx.android.synthetic.main.floating_action_button.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import onlymash.flexbooru.ap.R
import onlymash.flexbooru.ap.common.QUERY_KEY
import onlymash.flexbooru.ap.common.SETTINGS_NIGHT_MODE_KEY
import onlymash.flexbooru.ap.common.Settings
import onlymash.flexbooru.ap.common.USER_UID_KEY
import onlymash.flexbooru.ap.data.db.UserManager
import onlymash.flexbooru.ap.data.db.dao.DetailDao
import onlymash.flexbooru.ap.data.model.User
import onlymash.flexbooru.ap.ui.base.BaseActivity
import onlymash.flexbooru.ap.ui.fragment.*
import org.kodein.di.generic.instance

class MainActivity : BaseActivity(), SharedPreferences.OnSharedPreferenceChangeListener {

    private val sp by instance<SharedPreferences>()
    private val detailDao by instance<DetailDao>()

    private lateinit var appBarConfiguration: AppBarConfiguration

    @IdRes
    private var currentFragmentId = R.id.nav_posts

    private var user: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        sp.registerOnSharedPreferenceChangeListener(this)
        setSupportActionBar(toolbar)
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_posts,
                R.id.nav_history,
                R.id.nav_settings
            ),
            drawer_layout
        )
        val navController = findNavController(R.id.nav_host_fragment)
        setupActionBarWithNavController(navController, appBarConfiguration)
        nav_view.setupWithNavController(navController)
        navController.addOnDestinationChangedListener { _, destination, _ ->
            currentFragmentId = destination.id
            val lp = toolbar.layoutParams as AppBarLayout.LayoutParams
            when (currentFragmentId) {
                R.id.nav_posts,
                R.id.nav_history -> {
                    lp.scrollFlags = AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS
                    fab.visibility = View.VISIBLE
                }
                else -> {
                    lp.scrollFlags = AppBarLayout.LayoutParams.SCROLL_FLAG_NO_SCROLL
                    fab.visibility = View.GONE
                }
            }
            delegate.invalidateOptionsMenu()
        }
        fab.setOnClickListener {
            when (currentFragmentId) {
                R.id.nav_posts -> {
                    sendBroadcast(
                        Intent(JUMP_TO_TOP_ACTION_FILTER_KEY)
                            .putExtra(JUMP_TO_TOP_KEY, true)
                            .putExtra(JUMP_TO_TOP_QUERY_KEY, "")
                    )
                }
                R.id.nav_history -> {
                    sendBroadcast(
                        Intent(HISTORY_JUMP_TO_TOP_ACTION_FILTER_KEY)
                            .putExtra(HISTORY_JUMP_TO_TOP_KEY, true)
                    )
                }
            }
        }
        loadUser()
        nav_view.getHeaderView(0)?.setOnClickListener {
            if (user == null) {
                startActivity(Intent(this, LoginActivity::class.java))
            } else {
                startActivity(Intent(this, UserActivity::class.java))
            }
        }
    }

    private fun loadUser() {
        lifecycleScope.launch {
            user = withContext(Dispatchers.IO) {
                UserManager.getUserByUid(Settings.userUid)
            }
            when (val u = user) {
                null -> {

                }
                else -> {
                    u.uid
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        when (currentFragmentId) {
            R.id.nav_posts -> {
                menuInflater.inflate(R.menu.posts, menu)
                val item = menu?.findItem(R.id.action_search) ?: return true
                val searchView = item.actionView as SearchView
                initSearchView(searchView)
            }
            R.id.nav_history -> menuInflater.inflate(R.menu.history, menu)
            R.id.nav_settings -> menu?.clear()
        }
        return true
    }

    private fun initSearchView(searchView: SearchView) {
        searchView.queryHint = getString(R.string.search_posts_hint)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrEmpty()) return false
                return true
            }
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (query.isNullOrEmpty()) return false
                search(query)
                return true
            }
        })
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.action_clear_all_history -> {
                AlertDialog.Builder(this)
                    .setTitle(R.string.history_clear_all)
                    .setMessage(R.string.history_clear_all_content)
                    .setPositiveButton(R.string.dialog_ok) { _, _ ->
                        lifecycleScope.launch(Dispatchers.IO) {
                            detailDao.deleteAll()
                        }
                    }
                    .setNegativeButton(R.string.dialog_cancel, null)
                    .create()
                    .show()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            SETTINGS_NIGHT_MODE_KEY -> AppCompatDelegate.setDefaultNightMode(Settings.nightMode)
            USER_UID_KEY -> loadUser()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        sp.unregisterOnSharedPreferenceChangeListener(this)
    }

    private fun search(query: String) {
        findNavController(R.id.nav_host_fragment).navigate(
            R.id.nav_search,
            Bundle().apply {
                putString(QUERY_KEY, query)
            }
        )
    }
}