package com.example.attendance.fragments.snapmit.assignments

import android.Manifest.permission
import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.Environment
import android.text.Html
import android.text.SpannableStringBuilder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.Transformation
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.viewModelScope
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.example.attendance.MainActivity
import com.example.attendance.R
import com.example.attendance.adapters.snapmit.ZoomableImageAdapter
import com.example.attendance.models.snapmit.Assignment
import com.example.attendance.models.snapmit.Submission
import com.example.attendance.util.android.Navigation
import com.example.attendance.util.android.ocr.MyImageProcessing
import com.example.attendance.util.android.onTextChange
import com.example.attendance.util.auth.currentUserEmail
import com.example.attendance.viewmodels.AssignmentsViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.itextpdf.text.*
import com.itextpdf.text.pdf.PdfWriter
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.CompositeMultiplePermissionsListener
import com.karumi.dexter.listener.multi.DialogOnAnyDeniedMultiplePermissionsListener
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import kotlinx.android.synthetic.main.fragment_submission_view.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.opencv.imgcodecs.Imgcodecs
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.concurrent.atomic.AtomicInteger

/**
 * A simple [Fragment] subclass.
 */
class SubmissionViewFragment : Fragment() {
    private val assignmentsViewModel: AssignmentsViewModel by activityViewModels()
    private var comment = ""
    private var commentInit: String? = ""
    private lateinit var submission: Submission
    private lateinit var assignment: Assignment

    private fun updateDisplay(root: View? = view) {
        root ?: return
        val isOwner = assignment.owner == currentUserEmail()
        if (comment == commentInit) {
            comment = submission.comment
            commentInit = submission.comment
            root.edit_comment_box.setText(comment)
        }
        with(root) {
            toolbarMain.title = assignment.name
            val currentItem = images.currentItem
            images.adapter = ZoomableImageAdapter(submission.images, this@SubmissionViewFragment)
            images.currentItem = currentItem
            val sfd = SimpleDateFormat("d MMMM yyyy  HH:mm")
            submissionDescription.text =
                "Submitted by ${submission.name}\nat " + sfd.format(submission.submissionTime.toDate())
            numPages.text = "${images.currentItem + 1}/${submission.images.size}"
            if (isOwner) {
                oasButton.visibility = View.VISIBLE
                comment_button.text = "Edit comment"
                email_button.visibility = View.VISIBLE
                edit_comment_box.isEnabled = true
                edit_comment_box.hint =
                    "Write a comment here! ${submission.name} will be able to see it!"
            } else {
                oasButton.visibility = View.GONE
                comment_button.text = "Show comment"
                email_button.visibility = View.GONE
                edit_comment_box.isEnabled = false
                edit_comment_box.hint = "There are currently no comments"
            }
        }

    }

    @SuppressLint("CheckResult")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val root =
            inflater.inflate(R.layout.fragment_submission_view, container, false)
        // Shouldn't NPE but could
        submission = assignmentsViewModel.getSubmission()!!
        assignment = assignmentsViewModel.getAssignment()!!
        updateDisplay(root)
        assignmentsViewModel.assignments.observe({ lifecycle }) {
            if (assignmentsViewModel.currentAssignmentId in it.map { assignment -> assignment.id }) {
                assignment = assignmentsViewModel.getAssignment()!!
                updateDisplay()
            } else {
                Snackbar.make(
                    container!!,
                    "You no longer have access to this assignment",
                    Snackbar.LENGTH_LONG
                ).show()
                assignmentsViewModel.currentAssignmentId = null
                assignmentsViewModel.currentSubmissionId = null
                Navigation.navigate(R.id.assignmentsListFragment)
            }
        }
        assignmentsViewModel.submissions.observe({ lifecycle }) {
            if (assignmentsViewModel.currentSubmissionId in it.map { submissions -> submissions.id }) {
                submission = assignmentsViewModel.getSubmission()!!
                updateDisplay()
            } else {
                Snackbar.make(
                    container!!,
                    "You no longer have access to this submission",
                    Snackbar.LENGTH_LONG
                ).show()
                assignmentsViewModel.currentSubmissionId = null
                Navigation.navigate(R.id.assignmentFragment)
            }
        }
        root.toolbarMain.apply {
            setNavigationIcon(R.drawable.ic_arrow_back_black_24dp)
            navigationIcon?.setTint(Color.WHITE)
            setNavigationOnClickListener {
                assignmentsViewModel.currentSubmissionId = null
                Navigation.navigate(R.id.assignmentFragment)
            }
        }


        with(root) {
            val viewPager = images
            backButton.setOnClickListener {
                val curr = viewPager.currentItem
                if (curr == 0) return@setOnClickListener
                viewPager.setCurrentItem(curr - 1, true)
                numPages.text = "${viewPager.currentItem + 1}/${submission.images.size}"

            }
            nextButton.setOnClickListener {
                val curr = viewPager.currentItem
                if (curr == viewPager.adapter!!.itemCount - 1) return@setOnClickListener
                viewPager.setCurrentItem(curr + 1, true)
                numPages.text = "${viewPager.currentItem + 1}/${submission.images.size}"
            }
            edit_comment_box.onTextChange {
                comment = it
            }
            comment_button.setOnClickListener { v: View ->
                if (comment_button.text == "Edit comment") {
                    comment_button.text = "Save comment"
                } else if (comment_button.text == "Save comment") {
                    comment_button.text = "Edit comment"
                    updateComment()
                } else if (comment_button.text == "Show comment") {
                    comment_button.text = "Hide comment"
                } else if (comment_button.text == "Hide comment") {
                    comment_button.text = "Show comment"
                }
                v.isClickable = false
                val a: Animation = object : Animation() {
                    var initHeight = edit_comment_box.height
                    var targetHeight = if (initHeight > 100) 0 else 600
                    override fun applyTransformation(
                        interpolatedTime: Float,
                        t: Transformation
                    ) {
                        edit_comment_box.layoutParams.height =
                            if (interpolatedTime == 1f) targetHeight else ((targetHeight - initHeight) * interpolatedTime).toInt() + initHeight
                        edit_comment_box.requestLayout()
                        root.findViewWithTag<View>(viewPager.currentItem)
                            .requestLayout()
                    }

                    override fun willChangeBounds(): Boolean {
                        return true
                    }
                }
                a.duration = 600
                a.setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationStart(animation: Animation) {}
                    override fun onAnimationEnd(animation: Animation) {
                        v.isClickable = true
                    }

                    override fun onAnimationRepeat(animation: Animation) {}
                })
                edit_comment_box.startAnimation(a)
            }
            email_button
                .setOnClickListener {
                    val intent = Intent(Intent.ACTION_SEND)
                    intent.type = "*/*"
                    intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(submission.owner))
                    intent.putExtra(
                        Intent.EXTRA_SUBJECT,
                        "Regarding " + assignmentsViewModel.getAssignment()!!.name
                    )
                    if (activity?.packageManager?.let { it1 -> intent.resolveActivity(it1) } != null) {
                        startActivity(intent)
                    }
                }
            export_button.setOnClickListener { v ->
                if (ContextCompat.checkSelfPermission(
                        MainActivity.activity,
                        permission.WRITE_EXTERNAL_STORAGE
                    )
                    != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                        MainActivity.activity,
                        permission.READ_EXTERNAL_STORAGE
                    )
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    val customListener: MultiplePermissionsListener =
                        object : MultiplePermissionsListener {
                            override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                                if (report.areAllPermissionsGranted()) root.findViewById<View>(
                                    R.id.export_button
                                ).callOnClick()
                            }

                            override fun onPermissionRationaleShouldBeShown(
                                permissions: List<PermissionRequest>,
                                token: PermissionToken
                            ) {
                            }
                        }
                    val failureListener: MultiplePermissionsListener =
                        DialogOnAnyDeniedMultiplePermissionsListener.Builder
                            .withContext(MainActivity.activity)
                            .withTitle("Storage permissions are needed")
                            .withButtonText(android.R.string.ok)
                            .withIcon(R.drawable.ic_menu_camera)
                            .build()
                    val listener: MultiplePermissionsListener =
                        CompositeMultiplePermissionsListener(customListener, failureListener)
                    Dexter.withActivity(MainActivity.activity)
                        .withPermissions(
                            permission.WRITE_EXTERNAL_STORAGE,
                            permission.READ_EXTERNAL_STORAGE
                        )
                        .withListener(listener)
                        .check()
                    return@setOnClickListener
                }
                val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
                val date = SimpleDateFormat("d MMMM yyyy  HH:mm")
                    .format(submission.submissionTime.toDate())
                var cutName: String = assignment.name.replace(Regex("[^a-zA-Z0-9]"), "")
                if (cutName.length > 10) cutName = cutName.substring(0, 10)
                val parent =
                    LayoutInflater.from(context).inflate(R.layout.styled_textview, null)
                val edit = parent.findViewById<TextView>(R.id.textView2)
                edit.text = submission.name + "_" + cutName
                val dialog =
                    MaterialAlertDialogBuilder(v.context, R.style.SuccessDialog)
                        .setIcon(R.drawable.ic_picture_as_pdf_black_24dp)
                        .setTitle("Export as PDF")
                        .setMessage("Export to a PDF file, and open in another app. The file will be called ")
                        .setView(parent)
                        .setPositiveButton(
                            "OK"
                        ) { _: DialogInterface?, _: Int ->
                            val pdfname = edit.text.toString()
                            if (pdfname.isEmpty()) {
                                Toast.makeText(
                                    context,
                                    "pdf name cannot be empty!",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@setPositiveButton
                            }
                            val file = File(dir, "$pdfname.pdf")
                            file.createNewFile()
                            val spinner = ProgressBar(v.context)
                            spinner.setPadding(16, 16, 16, 16)
                            val dialogLoad =
                                MaterialAlertDialogBuilder(v.context)
                                    .setTitle("Please wait")
                                    .setView(spinner)
                                    .create()
                            dialogLoad.setCancelable(false)
                            dialogLoad.setCanceledOnTouchOutside(false)
                            dialogLoad.show()
                            assignmentsViewModel.viewModelScope.launch(Dispatchers.IO) {
                                val document =
                                    Document()
                                try {
                                    PdfWriter.getInstance(
                                        document,
                                        file.outputStream()
                                    )
                                } catch (e: DocumentException) {
                                    e.printStackTrace()
                                } catch (e: FileNotFoundException) {
                                    e.printStackTrace()
                                }
                                try {
                                    with(document) {
                                        open()
                                        newPage()
                                        pageSize = PageSize.A4
                                        addAuthor(
                                            FirebaseAuth.getInstance().currentUser!!.displayName
                                        )
                                        addCreator("Snapmit-Attendance")
                                    }
                                    val comment = submission.comment
                                    var text =
                                        """
                            ${submission.name}'s submission
        
                            """.trimIndent()
                                    text += submission.owner + "\n"
                                    text += "Submitted at $date\n"
                                    text += """
                            Assignment title: ${assignment.name}
        
                            """.trimIndent()
                                    text += "Teacher's comment: " + if (comment.isEmpty()) "None" else comment
                                    document.add(Paragraph(text))
                                    val pages =
                                        AtomicInteger(0)
                                    for (url in submission.images) {
                                        Log.d("PDF", url)
                                        Glide.with(v).downloadOnly().load(url)
                                            .into(object : SimpleTarget<File>() {
                                                override fun onResourceReady(
                                                    resource: File,
                                                    transition: Transition<in File>?
                                                ) {
                                                    document.newPage()
                                                    try {
                                                        synchronized(document) {
                                                            val image =
                                                                Image.getInstance(
                                                                    resource.absolutePath
                                                                )
                                                            image.scaleToFit(PageSize.A4)
                                                            document.pageSize = PageSize.A4
                                                            document.add(image)
                                                        }
                                                    } catch (e: DocumentException) {
                                                        e.printStackTrace()
                                                    } catch (e: IOException) {
                                                        e.printStackTrace()
                                                    }
                                                    pages.incrementAndGet()
                                                }
                                            })
                                    }
                                    while (pages.get() != submission.images.size) {
                                        if (Thread.interrupted()) throw InterruptedException()
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                                document.close()
                                val intent = Intent(Intent.ACTION_VIEW)
                                intent.setDataAndType(
                                    FileProvider.getUriForFile(
                                        requireContext(),
                                        "com.example.attendance.provider",  //(use your app signature + ".provider" )
                                        file
                                    ), "application/pdf"
                                )
                                intent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY
                                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                startActivity(intent)
                                dialogLoad.dismiss()
                            }
                        }
                dialog.show()
            }
            oasButton.setOnClickListener {
                val curr: String =
                    (viewPager.adapter as ZoomableImageAdapter).images[viewPager.currentItem]
                Glide.with(requireContext()).asFile().load(curr)
                    .downloadOnly(object : SimpleTarget<File>() {
                        override fun onResourceReady(
                            resource: File,
                            transition: Transition<in File>?
                        ) {
                            val image = Imgcodecs.imread(resource.absolutePath)
                            val textview = TextView(context)
                            val spannable = SpannableStringBuilder("")
                            val title: String
                            try {
                                val scans: Array<String> =
                                    MyImageProcessing.readOASPage(image)
                                for (q in 0..19) {
                                    val get = scans[q]
                                    var col: String? = null
                                    if (get.contains("✔")) col = "3dd47a"
                                    if (get.contains("✖")) col = "d4484a"
                                    if (col == null) spannable.append("Q" + (q + 1) + ": " + get) else spannable.append(
                                        Html.fromHtml(
                                            "<font color=#" + col + ">" + "Q" + (q + 1) + ": " + get.substring(
                                                1
                                            ) + "</font>"
                                        )
                                    )
                                    spannable.append("\n")
                                }
                                title =
                                    if (scans[20] == "0/0") "OAS scan" else "OAS score: " + scans[20]
                                textview.text = spannable
                                MaterialAlertDialogBuilder(context, R.style.SuccessDialog)
                                    .setTitle(title)
                                    .setView(textview)
                                    .setIcon(R.drawable.ic_oas)
                                    .show()
                            } catch (e: Exception) {
                                e.printStackTrace()
                                MaterialAlertDialogBuilder(context, R.style.ErrorDialog)
                                    .setTitle("No OAS found")
                                    .setPositiveButton("Ok") { dialog: DialogInterface, _: Int ->
                                        dialog.dismiss()
                                    }
                                    .setIcon(R.drawable.ic_oas)
                                    .show()
                            }
                        }
                    })
            }
        }

        return root
    }

    private fun updateComment() {
        if (commentInit == null) return
        if (commentInit == comment) return
        assignmentsViewModel.updateComment(comment)
    }
}
