package com.duyvv.service.ui.music

import androidx.lifecycle.ViewModel
import com.duyvv.service.R
import com.duyvv.service.domain.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MusicViewModel : ViewModel() {

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs = _songs.asStateFlow()

    init {
        initSongs()
    }

    private fun initSongs() {
        _songs.value = listOf(
            Song("1 Phút", "Andiez", R.raw.mot_phut_andiez),
            Song("Attention", "Charlie Puth", R.raw.attention_charlie_puth),
            Song("Dusk Till Dawn", "ZAYN ft. Sia", R.raw.dusk_till_dawn_zayn_sia),
            Song("Dynasty", "MIIA", R.raw.dynasty_miia),
            Song("Hoa Nở Không Màu", "Hoài Lâm", R.raw.hoa_no_khong_mau_hoai_lam),
            Song("Khó vẽ nụ cười", "Đạt G & Du Uyên", R.raw.kho_ve_nu_cuoi),
            Song("Nàng Thơ", "Hoàng Dũng", R.raw.nang_tho_hoang_dung),
            Song(
                "Ngày Mai Người Ta Lấy Chồng",
                "Thành Đạt",
                R.raw.ngay_mai_nguoi_ta_lay_chong_thanh_dat
            ),
            Song("Như Những Phút Ban Đầu", "Hoài Lâm", R.raw.nhu_nhung_phut_ban_dau_hoai_lam),
            Song(
                "We Don't Talk Anymore",
                "Charlie Puth feat. Selena Gomez",
                R.raw.we_dont_talk_anymore
            ),
        )
    }
}