<script>
    import {_} from "../../../main.js";
    import {NoContent, Pagination} from "@panomc/sdk/components";
    import CommentRow from "../rows/CommentRow.svelte";

    export let comments = [];
    export let commentCount = 0;
    export let page = 1;
    export let totalPage = 1;
    export let onDelete;
    export let onPageClick;
    export let highlightedComment = null;
</script>

<div class="card">
    <div class="card-header">
        {$_("pages.comments.table-title", {
            values: {
                commentCount: commentCount || 0,
            },
        })}
    </div>

    {#if commentCount === 0}
        <NoContent/>
    {:else}
        <div class="table-responsive">
            <table class="table table-hover align-middle">
                <thead>
                <tr>
                    <th
                            scope="col"
                            class="align-middle"
                            style="width: 40px;"
                    ></th>
                    <th
                            class="align-middle text-nowrap"
                            style="width: 60px;"
                    >
                        {$_("pages.comments.table.id")}
                    </th>
                    <th
                            class="align-middle text-nowrap"
                            style="width: 200px;"
                    >
                        {$_("pages.comments.table.author")}
                    </th>
                    <th class="align-middle text-nowrap">
                        {$_("pages.comments.table.comment")}
                    </th>
                    <th
                            class="align-middle text-nowrap"
                            style="width: 100px;"
                    >
                        {$_("pages.comments.table.likes")}
                    </th>
                    <th
                            class="align-middle text-nowrap"
                            style="width: 150px;"
                    >
                        {$_("pages.comments.table.date")}
                    </th>
                </tr>
                </thead>
                <tbody>
                {#if highlightedComment}
                    <tr>
                        <td
                                colspan="6"
                                class="bg-info bg-opacity-10 text-info small fw-medium py-2"
                        >
                            <i class="fas fa-star me-2"></i>
                            {$_("pages.comments.table.highlighted-comment")}
                        </td>
                    </tr>
                    <CommentRow
                            comment={highlightedComment}
                            {onDelete}
                            isHighlightedBlock={true}
                    />
                    <tr>
                        <td
                                colspan="6"
                                class="p-0"
                                style="height: 10px; background: rgba(0,0,0,0.05)"
                        ></td>
                    </tr>
                {/if}
                    {#each comments as comment (comment.id)}
                        <CommentRow {comment} {onDelete} />
                    {/each}
                </tbody>
            </table>
        </div>

        <div class="card-footer">
            <Pagination
                {page}
                {totalPage}
                on:firstPageClick={() => onPageClick(1)}
                on:lastPageClick={() => onPageClick(totalPage)}
                on:pageLinkClick={(event) => onPageClick(event.detail.page)}
            />
        </div>
    {/if}
</div>
