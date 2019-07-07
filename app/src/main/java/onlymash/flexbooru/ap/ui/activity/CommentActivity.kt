package onlymash.flexbooru.ap.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import de.hdodenhof.circleimageview.CircleImageView
import io.noties.markwon.Markwon
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.image.glide.GlideImagesPlugin
import kotlinx.android.synthetic.main.activity_comment.*
import kotlinx.android.synthetic.main.app_bar.*
import kotlinx.coroutines.launch
import onlymash.flexbooru.ap.R
import onlymash.flexbooru.ap.common.POST_ID_KEY
import onlymash.flexbooru.ap.common.Settings
import onlymash.flexbooru.ap.data.NetworkState
import onlymash.flexbooru.ap.data.Status
import onlymash.flexbooru.ap.data.api.Api
import onlymash.flexbooru.ap.data.db.UserManager
import onlymash.flexbooru.ap.data.model.Comment
import onlymash.flexbooru.ap.data.repository.comment.CommentRepositoryImpl
import onlymash.flexbooru.ap.extension.*
import onlymash.flexbooru.ap.glide.GlideApp
import onlymash.flexbooru.ap.glide.GlideRequests
import onlymash.flexbooru.ap.ui.base.KodeinActivity
import onlymash.flexbooru.ap.ui.diffcallback.CommentDiffCallback
import onlymash.flexbooru.ap.ui.viewmodel.CommentViewModel
import onlymash.flexbooru.ap.widget.LinkTransformationMethod
import org.kodein.di.erased.instance

class CommentActivity : KodeinActivity() {

    companion object {
        fun startActivity(context: Context, postId: Int) {
            context.startActivity(Intent(context, CommentActivity::class.java).apply {
                putExtra(POST_ID_KEY, postId)
            })
        }
    }

    private val comments: MutableList<Comment> = mutableListOf()

    private val api by instance<Api>()
    private val repo by lazy { CommentRepositoryImpl(api) }

    private lateinit var commentViewModel: CommentViewModel
    private lateinit var commentAdapter: CommentAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comment)
        val postId = intent?.getIntExtra(POST_ID_KEY, -1) ?: -1
        (toolbar.layoutParams as AppBarLayout.LayoutParams).scrollFlags = AppBarLayout.LayoutParams.SCROLL_FLAG_NO_SCROLL
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setTitle(R.string.title_comments)
            subtitle = "Post: $postId"
        }
        val glide = GlideApp.with(this)
        val markdown = Markwon.builder(this)
            .usePlugin(GlideImagesPlugin.create(glide))
            .usePlugin(HtmlPlugin.create())
            .build()
        commentAdapter = CommentAdapter(glide = glide, markwon = markdown)
        comments_list.apply {
            layoutManager = LinearLayoutManager(this@CommentActivity, RecyclerView.VERTICAL, false)
            adapter = commentAdapter
            addItemDecoration(DividerItemDecoration(this@CommentActivity, RecyclerView.VERTICAL))
        }
        val scheme = Settings.scheme
        val host = Settings.hostname
        UserManager.getUserByUid(Settings.userUid)?.let { user ->
            commentViewModel = getViewModel(CommentViewModel(
                repo = repo,
                scheme = scheme,
                host = host,
                token = user.token)
            )
            commentViewModel.comments.observe(this, Observer {
                val oldItems = mutableListOf<Comment>()
                oldItems.addAll(comments)
                comments.clear()
                comments.addAll(it)
                val result = DiffUtil.calculateDiff(CommentDiffCallback(oldItems, comments))
                result.dispatchUpdatesTo(commentAdapter)
            })
            commentViewModel.status.observe(this, Observer {
                comments_refresh.isRefreshing = it == NetworkState.LOADING
                if (it != NetworkState.LOADED) {
                    status_container.toVisibility(true)
                    retry_button.toVisibility(it.status == Status.FAILED)
                    error_msg.toVisibility(it.msg != null)
                    error_msg.text = it.msg
                } else {
                    status_container.toVisibility(false)
                }
            })
            if (postId > 0) {
                commentViewModel.loadComments(postId)
            }
            comments_refresh.setOnRefreshListener {
                commentViewModel.loadComments(postId)
            }
            retry_button.setOnClickListener {
                status_container.toVisibility(false)
                commentViewModel.loadComments(postId)
            }
            comment_send.setOnClickListener {
                val text = comment_edit.text?.toString() ?: ""
                if (text.length >= 2) {
                    lifecycleScope.launch {
                        when (
                            val result = repo.createComment(
                                url = getCreateCommentUrl(scheme, host, postId),
                                text = text,
                                token = user.token
                            )) {

                            is NetResult.Success -> {
                                commentViewModel.loadComments(postId)
                                comment_edit.text?.clear()
                            }
                            is NetResult.Error -> {
                                Toast.makeText(this@CommentActivity, result.errorMsg, Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            }
            user_avatar.setOnClickListener {
                startActivity(Intent(this, UserActivity::class.java))
            }
            val avatarUrl = user.avatarUrl
            if (!avatarUrl.isNullOrEmpty()) {
                glide.load(avatarUrl)
                    .placeholder(ContextCompat.getDrawable(this, R.drawable.avatar_user))
                    .into(user_avatar)
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    inner class CommentAdapter(private val glide: GlideRequests,
                               private val markwon: Markwon) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            CommentViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_comment, parent, false))

        override fun getItemCount(): Int = comments.size

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            (holder as CommentViewHolder).bindTo(comments[position])
        }

        inner class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

            private val avatar: CircleImageView = itemView.findViewById(R.id.user_avatar)
            private val username: AppCompatTextView = itemView.findViewById(R.id.user_name)
            private val date: AppCompatTextView = itemView.findViewById(R.id.date_text)
            private val commentView: AppCompatTextView = itemView.findViewById(R.id.comment_text)
            private var comment: Comment? = null

            init {
                itemView.setOnClickListener {
                    comment?.let {
                        UserActivity.startUserActivity(
                            context = this@CommentActivity,
                            userId = it.user.id,
                            username = it.user.userName,
                            avatarUrl = it.user.userAvatar
                        )
                    }
                }
            }

            fun bindTo(data: Comment) {
                comment = data
                username.text = data.user.userName
                date.text = data.comment.datetime.formatDate()
                markwon.setMarkdown(commentView, data.comment.html)
                commentView.transformationMethod = LinkTransformationMethod()
                data.user.userAvatar?.let {
                    glide.load(it)
                        .placeholder(ContextCompat.getDrawable(this@CommentActivity, R.drawable.avatar_user))
                        .into(avatar)
                }
            }
        }
    }
}
