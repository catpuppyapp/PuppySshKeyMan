package com.catpuppyapp.sshkeyman.screen.content.homescreen.scaffold.actions

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AllInbox
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import com.catpuppyapp.sshkeyman.compose.LongPressAbleIconBtn
import com.catpuppyapp.sshkeyman.constants.Cons
import com.catpuppyapp.sshkeyman.constants.PageRequest
import com.catpuppyapp.sshkeyman.data.entity.SshKeyEntity
import com.catpuppyapp.sshkeyman.dev.ignoreWorktreeFilesTestPassed
import com.catpuppyapp.sshkeyman.dev.proFeatureEnabled
import com.catpuppyapp.sshkeyman.dev.pushForceTestPassed
import com.catpuppyapp.sshkeyman.dev.rebaseTestPassed
import com.catpuppyapp.sshkeyman.play.pro.R
import com.catpuppyapp.sshkeyman.style.MyStyleKt
import com.catpuppyapp.sshkeyman.utils.AppModel
import com.catpuppyapp.sshkeyman.utils.UIHelper
import com.catpuppyapp.sshkeyman.utils.cache.Cache
import com.catpuppyapp.sshkeyman.utils.dbIntToBool
import com.catpuppyapp.sshkeyman.utils.state.CustomStateSaveable
import com.github.git24j.core.Repository
import kotlinx.coroutines.CoroutineScope

@Composable
fun ChangeListPageActions(
    changeListCurRepo: CustomStateSaveable<SshKeyEntity>,
    changeListRequireRefreshFromParentPage: () -> Unit,
    changeListHasIndexItems:MutableState<Boolean>,
//    requirePull:MutableState<Boolean>,
//    requirePush:MutableState<Boolean>,
    requireDoActFromParent:MutableState<Boolean>,
    requireDoActFromParentShowTextWhenDoingAct:MutableState<String>,

    //是否启用按钮，一般来说，执行操作时，会把与当前操作冲突的操作设为禁用
    enableAction:MutableState<Boolean>,

    repoState:MutableIntState,
    fromTo:String,

    listState: LazyListState,
    scope: CoroutineScope,
    changeListPageNoRepo:MutableState<Boolean>,
    hasNoConflictItems:Boolean,
    changeListPageFilterModeOn:MutableState<Boolean>,
    changeListPageFilterKeyWord:CustomStateSaveable<TextFieldValue>,
    rebaseCurOfAll:String,
    naviTarget:MutableState<String>,
) {
    val isWorktreePage = fromTo == Cons.gitDiffFromIndexToWorktree
    val navController = AppModel.singleInstanceHolder.navController
    val activityContext = AppModel.singleInstanceHolder.activityContext
    val dropDownMenuExpendState = rememberSaveable { mutableStateOf(false) }

    val repoIsDetached = dbIntToBool(changeListCurRepo.value.isDetached)
//    val hasTmpStatus = changeListCurRepo.value.tmpStatus.isNotBlank()  //为了避免和repoCard执行的操作冲突，检查下此变量
    val repoUnderMerge = repoState.intValue == Repository.StateT.MERGE.bit

    if(isWorktreePage) {
        LongPressAbleIconBtn(
            enabled = enableAction.value && !changeListPageNoRepo.value,
            tooltipText = stringResource(R.string.index),
            icon = if(changeListHasIndexItems.value) Icons.Filled.AllInbox else Icons.Filled.Inbox,
            iconContentDesc = stringResource(R.string.index),
            iconColor = UIHelper.getIconEnableColorOrNull(changeListHasIndexItems.value)
        ) {
            navController.navigate(Cons.nav_IndexScreen)
        }
    }

    LongPressAbleIconBtn(
        enabled = enableAction.value && !changeListPageNoRepo.value,

        tooltipText = stringResource(R.string.filter),
        icon =  Icons.Filled.FilterAlt,
        iconContentDesc = stringResource(id = R.string.filter),

    ) {
        changeListPageFilterKeyWord.value= TextFieldValue("")
        changeListPageFilterModeOn.value = true
    }

//    LongPressAbleIconBtn(
//        enabled = enableAction.value && !changeListPageNoRepo.value,
//
//        tooltipText = stringResource(R.string.go_to_top),
//        icon =  Icons.Filled.VerticalAlignTop,
//        iconContentDesc = stringResource(id = R.string.go_to_top),
//
//    ) {
//        UIHelper.scrollToItem(scope, listState, 0)
//    }

    LongPressAbleIconBtn(
        enabled = enableAction.value,

        tooltipText = stringResource(R.string.refresh),
        icon = Icons.Filled.Refresh,
        iconContentDesc = stringResource(R.string.refresh),
    ) {
        changeListRequireRefreshFromParentPage()
    }

    //菜单图标
    LongPressAbleIconBtn(
        //这种需展开的菜单，禁用内部的选项即可
//        enabled = enableAction.value,

        tooltipText = stringResource(R.string.menu),
        icon = Icons.Filled.MoreVert,
        iconContentDesc = stringResource(R.string.menu),
        onClick = {
            //切换菜单展开状态
            dropDownMenuExpendState.value = !dropDownMenuExpendState.value
        }
    )
    Row(modifier = Modifier.padding(top = MyStyleKt.TopBar.dropDownMenuTopPaddingSize)) {
//        val enableMenuItem = enableAction.value && !changeListPageNoRepo.value  && !hasTmpStatus
        val enableMenuItem = enableAction.value && !changeListPageNoRepo.value
        //菜单列表
        DropdownMenu(
            expanded = dropDownMenuExpendState.value,
            onDismissRequest = { dropDownMenuExpendState.value=false }
        ) {
            if(isWorktreePage) {  // stage all for worktree page
                DropdownMenuItem(
                    enabled = enableMenuItem,

                    text = { Text(stringResource(R.string.stage_all)) },
                    onClick = {
                        Cache.set(Cache.Key.changeListInnerPage_requireDoActFromParent, PageRequest.stageAll)
                        requireDoActFromParentShowTextWhenDoingAct.value = activityContext.getString(R.string.staging)
                        requireDoActFromParent.value = true
                        enableAction.value=false  //禁用顶栏的按钮，避免用户重复操作，不过libgit2应该本身有避免重复执行会冲突的操作的机制，但我最好还是再控制一下，避免发生冲突才是最佳

                        dropDownMenuExpendState.value=false
                    }
                )
            }else {  //commit for index page
                DropdownMenuItem(
                    enabled = enableMenuItem,

                    text = { Text(stringResource(R.string.commit)) },
                    onClick = {
                        Cache.set(Cache.Key.changeListInnerPage_requireDoActFromParent, PageRequest.commit)
                        requireDoActFromParentShowTextWhenDoingAct.value = activityContext.getString(R.string.committing)
                        requireDoActFromParent.value = true
                        enableAction.value=false  //禁用顶栏的按钮，避免用户重复操作，不过libgit2应该本身有避免重复执行会冲突的操作的机制，但我最好还是再控制一下，避免发生冲突才是最佳

                        dropDownMenuExpendState.value=false
                    }
                )
            }

            val enableRepoAction = enableMenuItem && !repoIsDetached

            DropdownMenuItem(
                enabled = enableRepoAction,

                text = { Text(stringResource(R.string.fetch)) },
                onClick = {
                    Cache.set(Cache.Key.changeListInnerPage_requireDoActFromParent, PageRequest.fetch)
                    requireDoActFromParentShowTextWhenDoingAct.value = activityContext.getString(R.string.fetching)
                    requireDoActFromParent.value = true
                    enableAction.value=false  //禁用顶栏的按钮，避免用户重复操作，不过libgit2应该本身有避免重复执行会冲突的操作的机制，但我最好还是再控制一下，避免发生冲突才是最佳

                    dropDownMenuExpendState.value=false
                }
            )
            DropdownMenuItem(
                enabled = enableRepoAction,

                text = { Text(stringResource(R.string.pull)) },
                onClick = {
                    Cache.set(Cache.Key.changeListInnerPage_requireDoActFromParent, PageRequest.pull)
                    requireDoActFromParentShowTextWhenDoingAct.value = activityContext.getString(R.string.pulling)
                    requireDoActFromParent.value = true
                    enableAction.value=false  //禁用顶栏的按钮，避免用户重复操作，不过libgit2应该本身有避免重复执行会冲突的操作的机制，但我最好还是再控制一下，避免发生冲突才是最佳

                    dropDownMenuExpendState.value=false
                }
            )

            if(proFeatureEnabled(rebaseTestPassed)) {
                DropdownMenuItem(
                    enabled = enableRepoAction,

                    text = { Text(stringResource(R.string.pull_rebase)) },
                    onClick = {
                        Cache.set(Cache.Key.changeListInnerPage_requireDoActFromParent, PageRequest.pullRebase)
                        requireDoActFromParentShowTextWhenDoingAct.value = activityContext.getString(R.string.pulling)
                        requireDoActFromParent.value = true
                        enableAction.value=false  //禁用顶栏的按钮，避免用户重复操作，不过libgit2应该本身有避免重复执行会冲突的操作的机制，但我最好还是再控制一下，避免发生冲突才是最佳

                        dropDownMenuExpendState.value=false
                    }
                )

            }

            DropdownMenuItem(
                enabled = enableRepoAction,

                text = { Text(stringResource(R.string.push)) },
                onClick = {
                    Cache.set(Cache.Key.changeListInnerPage_requireDoActFromParent, PageRequest.push)
                    requireDoActFromParentShowTextWhenDoingAct.value=activityContext.getString(R.string.pushing)
                    requireDoActFromParent.value = true
                    enableAction.value=false

                    dropDownMenuExpendState.value=false
                }
            )
            if(proFeatureEnabled(pushForceTestPassed)) {
                DropdownMenuItem(
                    enabled = enableRepoAction,

                    text = { Text(stringResource(R.string.push_force)) },
                    onClick = {
                        Cache.set(Cache.Key.changeListInnerPage_requireDoActFromParent, PageRequest.pushForce)
                        requireDoActFromParentShowTextWhenDoingAct.value=activityContext.getString(R.string.force_pushing)
                        requireDoActFromParent.value = true
                        enableAction.value=false

                        dropDownMenuExpendState.value=false
                    }
                )
            }
            DropdownMenuItem(
                enabled = enableRepoAction,

                text = { Text(stringResource(R.string.sync)) },
                onClick = {
                    Cache.set(Cache.Key.changeListInnerPage_requireDoActFromParent, PageRequest.sync)
                    requireDoActFromParentShowTextWhenDoingAct.value= activityContext.getString(R.string.syncing)
                    requireDoActFromParent.value = true
                    enableAction.value=false

                    dropDownMenuExpendState.value=false
                }
            )

            if(proFeatureEnabled(rebaseTestPassed)) {
                DropdownMenuItem(
                    enabled = enableRepoAction,

                    text = { Text(stringResource(R.string.sync_rebase)) },
                    onClick = {
                        Cache.set(Cache.Key.changeListInnerPage_requireDoActFromParent, PageRequest.syncRebase)
                        requireDoActFromParentShowTextWhenDoingAct.value= activityContext.getString(R.string.syncing)
                        requireDoActFromParent.value = true
                        enableAction.value=false

                        dropDownMenuExpendState.value=false
                    }
                )

            }


            if(isWorktreePage) {
                if(proFeatureEnabled(ignoreWorktreeFilesTestPassed)) {
                    DropdownMenuItem(
//                    enabled = enableRepoAction,
//                        enabled = enableMenuItem,
                        enabled = true,

                        text = { Text(stringResource(R.string.edit_ignore_file)) },
                        onClick = {
                            Cache.set(Cache.Key.changeListInnerPage_requireDoActFromParent, PageRequest.editIgnoreFile)
                            requireDoActFromParentShowTextWhenDoingAct.value= activityContext.getString(R.string.loading)
                            requireDoActFromParent.value = true
                            enableAction.value=false

                            dropDownMenuExpendState.value=false
                        }
                    )
                }


                DropdownMenuItem(
//                    enabled = enableRepoAction,
//                    enabled = enableMenuItem,
                    enabled = true,  // go to repo page anytime, even loading something

                    text = { Text(stringResource(R.string.show_in_repos)) },
                    onClick = {
                        Cache.set(Cache.Key.changeListInnerPage_requireDoActFromParent, PageRequest.showInRepos)
                        requireDoActFromParentShowTextWhenDoingAct.value= activityContext.getString(R.string.loading)
                        requireDoActFromParent.value = true
                        enableAction.value=false

                        dropDownMenuExpendState.value=false
                    }
                )

                if(changeListCurRepo.value.parentRepoId.isNotBlank()) {
                    DropdownMenuItem(
//                    enabled = enableRepoAction,
//                    enabled = enableMenuItem,
                        enabled = true,

                        text = { Text(stringResource(R.string.go_parent)) },
                        onClick = {
                            Cache.set(Cache.Key.changeListInnerPage_requireDoActFromParent, PageRequest.goParent)
                            requireDoActFromParentShowTextWhenDoingAct.value= activityContext.getString(R.string.loading)
                            requireDoActFromParent.value = true
                            enableAction.value=false

                            dropDownMenuExpendState.value=false
                        }
                    )
                }
            }



            //merge相关
            if(repoUnderMerge) {
                DropdownMenuItem(
                    //如果正在执行其他操作，还是应该禁用下abortMerge，所以这也需要判断enableAction是否为true
//                    enabled = enableMenuItem && hasNoConflictItems && repoUnderMerge,  //废弃有冲突则禁用的方案，容易让用户困惑“怎么才能continue merge？”，改成点击后再检测是否有冲突了，若有，提示用户先解决冲突，这样用户就能通过和app互动来得知怎么 continue merge了
                    enabled = enableMenuItem,
                    text = { Text(stringResource(R.string.merge_continue)) },
                    onClick = {
                        Cache.set(Cache.Key.changeListInnerPage_requireDoActFromParent, PageRequest.mergeContinue)
                        requireDoActFromParentShowTextWhenDoingAct.value= activityContext.getString(R.string.continue_merging)
                        requireDoActFromParent.value = true
                        enableAction.value=false

                        dropDownMenuExpendState.value=false
                    }
                )

                DropdownMenuItem(
                    //如果正在执行其他操作，还是应该禁用下abortMerge，所以这也需要判断enableAction是否为true
//                    enabled = enableMenuItem && repoUnderMerge,
                    enabled = enableMenuItem,
                    text = { Text(stringResource(R.string.merge_abort)) },
                    onClick = {
                        Cache.set(Cache.Key.changeListInnerPage_requireDoActFromParent, PageRequest.mergeAbort)
                        requireDoActFromParentShowTextWhenDoingAct.value= activityContext.getString(R.string.aborting_merge)
                        requireDoActFromParent.value = true
                        enableAction.value=false

                        dropDownMenuExpendState.value=false
                    }
                )
            }

            //repo under rebase
            if(repoState.intValue == Repository.StateT.REBASE_MERGE.bit) {
                DropdownMenuItem(
                    //如果正在执行其他操作，还是应该禁用下abortMerge，所以这也需要判断enableAction是否为true
//                    enabled = enableMenuItem && repoUnderMerge,
                    enabled = enableMenuItem,

                    text = { Text(stringResource(R.string.rebase_continue)+rebaseCurOfAll) },
                    onClick = {
                        Cache.set(Cache.Key.changeListInnerPage_requireDoActFromParent, PageRequest.rebaseContinue)
                        requireDoActFromParentShowTextWhenDoingAct.value= activityContext.getString(R.string.rebase_continue)
                        requireDoActFromParent.value = true
                        enableAction.value=false

                        dropDownMenuExpendState.value=false
                    }
                )

                DropdownMenuItem(
                    //如果正在执行其他操作，还是应该禁用下abortMerge，所以这也需要判断enableAction是否为true
//                    enabled = enableMenuItem && hasNoConflictItems && repoUnderMerge,  //废弃有冲突则禁用的方案，容易让用户困惑“怎么才能continue merge？”，改成点击后再检测是否有冲突了，若有，提示用户先解决冲突，这样用户就能通过和app互动来得知怎么 continue merge了
                    enabled = enableMenuItem,

                    text = { Text(stringResource(R.string.rebase_skip)) },
                    onClick = {
                        Cache.set(Cache.Key.changeListInnerPage_requireDoActFromParent, PageRequest.rebaseSkip)
                        requireDoActFromParentShowTextWhenDoingAct.value= activityContext.getString(R.string.rebase_skip)
                        requireDoActFromParent.value = true
                        enableAction.value=false

                        dropDownMenuExpendState.value=false
                    }
                )
                DropdownMenuItem(
                    //如果正在执行其他操作，还是应该禁用下abortMerge，所以这也需要判断enableAction是否为true
//                    enabled = enableMenuItem && hasNoConflictItems && repoUnderMerge,  //废弃有冲突则禁用的方案，容易让用户困惑“怎么才能continue merge？”，改成点击后再检测是否有冲突了，若有，提示用户先解决冲突，这样用户就能通过和app互动来得知怎么 continue merge了
                    enabled = enableMenuItem,

                    text = { Text(stringResource(R.string.rebase_abort)) },
                    onClick = {
                        Cache.set(Cache.Key.changeListInnerPage_requireDoActFromParent, PageRequest.rebaseAbort)
                        requireDoActFromParentShowTextWhenDoingAct.value= activityContext.getString(R.string.rebase_abort)
                        requireDoActFromParent.value = true
                        enableAction.value=false

                        dropDownMenuExpendState.value=false
                    }
                )
            }

            if(repoState.intValue == Repository.StateT.CHERRYPICK.bit) {
                DropdownMenuItem(
                    //如果正在执行其他操作，还是应该禁用下abortMerge，所以这也需要判断enableAction是否为true
//                    enabled = enableMenuItem && hasNoConflictItems && repoUnderMerge,  //废弃有冲突则禁用的方案，容易让用户困惑“怎么才能continue merge？”，改成点击后再检测是否有冲突了，若有，提示用户先解决冲突，这样用户就能通过和app互动来得知怎么 continue merge了
                    enabled = enableMenuItem,

                    text = { Text(stringResource(R.string.cherrypick_continue)) },
                    onClick = {
                        Cache.set(Cache.Key.changeListInnerPage_requireDoActFromParent, PageRequest.cherrypickContinue)
                        requireDoActFromParentShowTextWhenDoingAct.value= activityContext.getString(R.string.cherrypick_continue)
                        requireDoActFromParent.value = true
                        enableAction.value=false

                        dropDownMenuExpendState.value=false
                    }
                )

                DropdownMenuItem(
                    //如果正在执行其他操作，还是应该禁用下abortMerge，所以这也需要判断enableAction是否为true
//                    enabled = enableMenuItem && hasNoConflictItems && repoUnderMerge,  //废弃有冲突则禁用的方案，容易让用户困惑“怎么才能continue merge？”，改成点击后再检测是否有冲突了，若有，提示用户先解决冲突，这样用户就能通过和app互动来得知怎么 continue merge了
                    enabled = enableMenuItem,

                    text = { Text(stringResource(R.string.cherrypick_abort)) },
                    onClick = {
                        Cache.set(Cache.Key.changeListInnerPage_requireDoActFromParent, PageRequest.cherrypickAbort)
                        requireDoActFromParentShowTextWhenDoingAct.value= activityContext.getString(R.string.cherrypick_abort)
                        requireDoActFromParent.value = true
                        enableAction.value=false

                        dropDownMenuExpendState.value=false
                    }
                )
            }

        }
    }

}