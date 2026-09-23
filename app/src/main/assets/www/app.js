const categories=[
  ['Todos','▦'],['Cama, Mesa e Banho','🛏️'],['Brinquedos','🧸'],['Utilidades','🏠'],
  ['Material Escolar','📚'],['Festas','🎉'],['Natal','🎄'],['Sazonais','☀️'],['Ferramentas','🔧']
];
let active='Todos', catalogs=[];
const client=supabase.createClient(window.SOMAR_CONFIG.SUPABASE_URL,window.SOMAR_CONFIG.SUPABASE_ANON_KEY);

function esc(v){return String(v??'').replace(/[&<>"']/g,m=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#039;'}[m]))}
function js(v){return JSON.stringify(String(v??''))}

const categoryImages={
  'Ferramentas':'capa-ferramentas.png'
};

function renderCats(){
  document.getElementById('cats').innerHTML=categories.map(x=>`
    <button class="cat ${x[0]===active?'active':''}" onclick='selectCat(${js(x[0])})'>
      <span class="cat-icon">${x[1]}</span><span>${esc(x[0])}</span>
    </button>`).join('');
}
function selectCat(x){active=x;renderCats();render();document.getElementById('title').textContent=x==='Todos'?'Catálogos':x}

function coverMarkup(c, detail=false){
  if(c.cover_url) return `<img src="${esc(c.cover_url)}" alt="Capa de ${esc(c.name)}" loading="lazy">`;
  const local=categoryImages[c.category];
  if(local) return `<img class="catalog-cover-image" src="${local}" alt="Capa de ${esc(c.name)}" loading="lazy">`;
  return `<div class="fallback-cover"><div class="fallback-icon">▦</div><strong>${esc(c.name)}</strong><span>${esc(c.category||'Catálogo')}</span></div>`;
}

function render(){
  const q=document.getElementById('search').value.toLowerCase().trim();
  const list=catalogs.filter(c=>(active==='Todos'||c.category===active)&&(!q||`${c.name} ${c.brand} ${c.category} ${c.description||''}`.toLowerCase().includes(q)));
  document.getElementById('count').textContent=list.length;
  document.getElementById('grid').innerHTML=list.length?list.map(c=>`
    <article class="card">
      <button class="card-main" onclick='openCatalog(${js(c.id)})'>
        <div class="cover">${coverMarkup(c)}<span class="tag">${esc(c.category)}</span></div>
        <div class="body">
          <div class="brand-name">${esc(c.brand||'')}</div>
          <h3>${esc(c.name)}</h3>
          <p>${esc(c.description||'Consulte o catálogo de produtos.')}</p>
        </div>
      </button>
      <div class="card-actions">
        <button class="btn primary" onclick='openCatalog(${js(c.id)})'>Ver catálogo</button>
        <button class="btn" onclick='shareCatalogById(${js(c.id)})'>Compartilhar</button>
      </div>
    </article>`).join(''):
    '<div class="empty"><strong>Nenhum catálogo encontrado.</strong><span>Tente outra busca ou selecione “Todos”.</span></div>';
}

function getCatalog(id){return catalogs.find(c=>String(c.id)===String(id))}
function openCatalog(id){
  const c=getCatalog(id); if(!c)return;
  const rawUrl=c.pdf_url||'';
  const url=esc(rawUrl||'#');
  document.getElementById('modal').innerHTML=`
    <div class="modal-backdrop" onclick="closeCatalog(event)">
      <div class="modal" onclick="event.stopPropagation()">
        <button class="close" onclick="closeCatalog()" aria-label="Fechar">×</button>
        <div class="detail-cover">${coverMarkup(c,true)}</div>
        <div class="detail-content">
          <span class="detail-tag">${esc(c.category)}</span>
          ${c.brand?`<div class="detail-brand">${esc(c.brand)}</div>`:''}
          <h2>${esc(c.name)}</h2>
          <p>${esc(c.description||'Consulte o catálogo de produtos.')}</p>
          <div class="detail-actions">
            <button class="btn primary big" onclick='openCatalogPdf(${js(c.id)})'>◉&nbsp; Ver catálogo</button>
            <a class="btn big" href="${url}${url.includes('?')?'&':'?'}download=true">⇩&nbsp; Baixar PDF</a>
            <button class="btn big share-button" onclick='shareCatalogById(${js(c.id)})'>⌯&nbsp; Compartilhar</button>
          </div>
        </div>
      </div>
    </div>`;
  document.getElementById('modal').classList.add('open');
  document.body.classList.add('modal-open');
}
function closeCatalog(e){if(e&&e.target!==e.currentTarget)return;document.getElementById('modal').classList.remove('open');document.body.classList.remove('modal-open')}

function openCatalogPdf(id){
  const c=getCatalog(id); if(!c||!c.pdf_url)return;
  if(window.AndroidOpen&&typeof window.AndroidOpen.openUrl==='function'){
    window.AndroidOpen.openUrl(c.pdf_url);
    return;
  }
  window.open(c.pdf_url,'_blank','noopener');
}

async function shareCatalogById(id){
  const c=getCatalog(id); if(!c)return;
  const url=c.pdf_url||location.href;
  const text=`Olá! Segue o catálogo ${c.name} da Somar Representações. ${url}`;
  if(window.AndroidShare&&typeof window.AndroidShare.shareText==='function'){
    window.AndroidShare.shareText(text);
    return;
  }
  if(navigator.share){
    try{await navigator.share({title:c.name,text,url});return}catch(e){}
  }
  const wa='https://wa.me/?text='+encodeURIComponent(text);
  window.location.href=wa;
}

async function loadCatalogs(){
  document.getElementById('status').textContent='Carregando catálogos...';
  const {data,error}=await client.from('catalogs').select('*').eq('published',true).order('created_at',{ascending:false});
  if(error){console.error(error);document.getElementById('status').textContent='Não foi possível carregar os catálogos agora.';return}
  catalogs=data||[];document.getElementById('status').textContent='';render();
}
function focusSearch(){document.getElementById('search').focus();document.getElementById('search').scrollIntoView({behavior:'smooth',block:'center'})}
document.getElementById('search').addEventListener('input',render);
document.addEventListener('keydown',e=>{if(e.key==='Escape')closeCatalog()});
renderCats();loadCatalogs();
