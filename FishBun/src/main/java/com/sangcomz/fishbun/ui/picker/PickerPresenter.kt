package com.sangcomz.fishbun.ui.picker

import android.net.Uri
import android.os.Environment
import com.sangcomz.fishbun.ui.picker.model.PickerListItem
import com.sangcomz.fishbun.ui.picker.model.PickerMenuViewData
import com.sangcomz.fishbun.ui.picker.model.PickerRepository
import com.sangcomz.fishbun.util.UiHandler
import com.sangcomz.fishbun.util.future.CallableFutureTask
import com.sangcomz.fishbun.util.future.FutureCallback
import java.util.concurrent.ExecutionException

/**
 * Created by sangcomz on 2015-11-05.
 */
class PickerPresenter internal constructor(
    private val pickerView: PickerContract.View,
    private val pickerRepository: PickerRepository,
    private val uiHandler: UiHandler
) : PickerContract.Presenter {

    private var imageListFuture: CallableFutureTask<List<Uri>>? = null
    private var dirPathFuture: CallableFutureTask<String>? = null

    override fun addAddedPath(addedImagePath: Uri) {
        pickerRepository.addAddedPath(addedImagePath)
    }

    override fun addAllAddedPath(addedImagePathList: List<Uri>) {
        pickerRepository.addAllAddedPath(addedImagePathList)
    }

    override fun getAddedImagePathList() = pickerRepository.getAddedPathList()

    override fun getPickerListItem() {
        val albumData = pickerRepository.getPickerAlbumData() ?: return

        imageListFuture = getAllMediaThumbnailsPath(albumData.albumId)
            .also {
                it.execute(object : FutureCallback<List<Uri>> {
                    override fun onSuccess(result: List<Uri>) {
                        onSuccessAllMediaThumbnailsPath(result)
                    }
                })
            }
    }

    fun onSuccessAllMediaThumbnailsPath(imageUriList: List<Uri>) {
        pickerRepository.setCurrentPickerImageList(imageUriList)
        val viewData = pickerRepository.getPickerViewData()
        val selectedImageList = pickerRepository.getSelectedImageList().toMutableList()
        val pickerList = arrayListOf<PickerListItem>()
        if (pickerRepository.hasCameraInPickerPage()) {
            pickerList.add(PickerListItem.Camera)
        }

        imageUriList.map {
            PickerListItem.Item(it, selectedImageList.indexOf(it), viewData)
        }.also {
            pickerList.addAll(it)
            uiHandler.run {
                pickerView.showImageList(pickerList, pickerRepository.getImageAdapter())
            }
        }
    }

    override fun transImageFinish() {
        val albumData = pickerRepository.getPickerAlbumData() ?: return

        pickerView.transImageFinish(albumData.albumPosition, pickerRepository.getAddedPathList())
    }

    override fun takePicture() {
        val albumData = pickerRepository.getPickerAlbumData() ?: return
        if (albumData.albumId == 0L) {
            pickerView.takePicture(
                Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DCIM + "/Camera"
                ).absolutePath
            )
        } else {
            try {
                dirPathFuture = pickerRepository.getDirectoryPath(albumData.albumId)
                    .also { pickerView.takePicture(it.get()) }
            } catch (e: ExecutionException) {
                e.printStackTrace()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }

        }
    }

    override fun successTakePicture(addedImagePath: Uri) {
        addAddedPath(addedImagePath)
    }

    override fun getDesignViewData() {
        val viewData = pickerRepository.getPickerViewData()
        with(pickerView) {
            initToolBar(viewData)
            initRecyclerView(viewData)
        }

        setToolbarTitle()
    }

    override fun onClickThumbCount(position: Int) {
        selectImage(position)
    }

    override fun onClickImage(position: Int) {
        if (pickerRepository.useDetailView()) {
            pickerView.showDetailView(getImagePosition(position))
        } else {
            selectImage(position)
        }
    }

    override fun onDetailImageActivityResult() {
        val pickerViewData = pickerRepository.getPickerViewData()
        if (pickerRepository.isLimitReached() && pickerViewData.isAutomaticClose) {
            pickerView.finishActivityWithResult(pickerRepository.getSelectedImageList())
        } else {
            getPickerListItem()
        }
    }

    override fun getPickerMenuViewData(callback: (PickerMenuViewData) -> Unit) {
        callback.invoke(pickerRepository.getPickerMenuViewData())
    }

    override fun onClickMenuDone() {
        if (pickerRepository.getSelectedImageList().size < pickerRepository.getMinCount()) {
            pickerView.showLimitReachedMessage(pickerRepository.getMessageLimitReached())
        } else {
            pickerView.finishActivity()
        }
    }

    override fun onClickMenuAllDone() {
        pickerRepository.getPickerImages().forEach {
            if (pickerRepository.isLimitReached()) {
                return@forEach
            }
            if (pickerRepository.isNotSelectedImage(it)) {
                pickerRepository.selectImage(it)
            }
        }
        pickerView.finishActivity()
    }

    override fun release() {
        dirPathFuture?.cancel(true)
        imageListFuture?.cancel(true)
    }

    private fun selectImage(position: Int) {
        if (pickerRepository.isLimitReached()) {
            pickerView.showLimitReachedMessage(pickerRepository.getMessageLimitReached())
            return
        }

        val imagePosition = getImagePosition(position)
        val imageUri = pickerRepository.getPickerImage(imagePosition)

        if (pickerRepository.isNotSelectedImage(imageUri)) {
            pickerRepository.selectImage(imageUri)
        } else {
            pickerRepository.unselectImage(imageUri)
        }

        pickerView.onCheckStateChange(
            position,
            PickerListItem.Item(
                imageUri,
                pickerRepository.getSelectedIndex(imageUri),
                pickerRepository.getPickerViewData()
            )
        )
        setToolbarTitle()
    }

    private fun setToolbarTitle() {
        val albumName = pickerRepository.getPickerAlbumData()?.albumName ?: ""
        pickerView.setToolbarTitle(
            pickerRepository.getPickerViewData(),
            pickerRepository.getSelectedImageList().size,
            albumName
        )
    }

    private fun getImagePosition(position: Int) =
        if (pickerRepository.hasCameraInPickerPage()) position - 1 else position

    private fun getAllMediaThumbnailsPath(
        albumId: Long,
        clearCache: Boolean = false
    ): CallableFutureTask<List<Uri>> {
        return pickerRepository.getAllBucketImageUri(albumId, clearCache)
    }
}