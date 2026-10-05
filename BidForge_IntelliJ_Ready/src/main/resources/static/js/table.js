document.addEventListener('DOMContentLoaded',()=>document.querySelectorAll('[data-table]').forEach(initTable));
function initTable(table){
  const tbody=table.tBodies[0];
  const rows=[...tbody.rows].filter(r=>!r.querySelector('.empty'));
  const root=table.closest('.card')||document;
  const search=root.querySelector('[data-table-search]');
  const size=root.querySelector('[data-page-size]');
  const pager=root.querySelector('[data-pagination]');
  let page=1;
  let pageSize=Number(size?.value||10);
  function render(){
    const q=(search?.value||'').trim().toLowerCase();
    const filtered=rows.filter(r=>r.innerText.toLowerCase().includes(q));
    const pages=Math.max(1,Math.ceil(filtered.length/pageSize));
    page=Math.min(page,pages);
    rows.forEach(r=>r.classList.add('d-none'));
    filtered.slice((page-1)*pageSize,page*pageSize).forEach(r=>r.classList.remove('d-none'));
    if(pager){
      const from=filtered.length?((page-1)*pageSize+1):0;
      const to=Math.min(page*pageSize,filtered.length);
      pager.innerHTML=`<button type="button" class="btn btn-outline-secondary btn-sm" ${page<=1?'disabled':''}>Previous</button><span class="page-info">Showing ${from}-${to} of ${filtered.length} · Page ${page} / ${pages}</span><button type="button" class="btn btn-outline-secondary btn-sm" ${page>=pages?'disabled':''}>Next</button>`;
      const buttons=pager.querySelectorAll('button');
      buttons[0]?.addEventListener('click',()=>{if(page>1){page--;render();}});
      buttons[1]?.addEventListener('click',()=>{if(page<pages){page++;render();}});
    }
  }
  search?.addEventListener('input',()=>{page=1;render();});
  size?.addEventListener('change',()=>{pageSize=Number(size.value);page=1;render();});
  render();
}
