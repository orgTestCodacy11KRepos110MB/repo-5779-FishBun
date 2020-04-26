package com.sangcomz.fishbun.ui.album.model.repository

import android.net.Uri
import com.sangcomz.fishbun.MimeType
import com.sangcomz.fishbun.adapter.image.ImageAdapter
import com.sangcomz.fishbun.ui.album.model.Album
import com.sangcomz.fishbun.ui.album.model.AlbumMenuViewData
import com.sangcomz.fishbun.ui.album.model.AlbumMetaData
import com.sangcomz.fishbun.ui.album.model.AlbumViewData
import com.sangcomz.fishbun.ui.detail.model.DetailImageViewData
import com.sangcomz.fishbun.util.future.CallableFutureTask
import java.util.concurrent.Future
import java.util.concurrent.FutureTask

interface AlbumRepository {
    fun getAlbumList(): CallableFutureTask<List<Album>>

    fun getAlbumMetaData(albumId: Long): CallableFutureTask<AlbumMetaData>

    fun getAlbumViewData(): AlbumViewData

    fun getImageAdapter(): ImageAdapter

    fun selectedImages(): List<Uri>

    fun getAlbumMenuViewData(): AlbumMenuViewData

    fun isNotEnoughSelectedImages(): Boolean

    fun getMessageNotingSelected(): String
}