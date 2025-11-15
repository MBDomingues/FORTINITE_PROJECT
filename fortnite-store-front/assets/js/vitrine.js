/**
 * Classe principal VitrineJS
 * Responsável por gerenciar toda a lógica do front-end da loja.
 */
class VitrineJS {
    constructor(userToken) {
        this.user = userToken || null;
        this.API_BASE_URL = 'http://130.213.12.104:8080/api/v1';
        
        this.itens = []; 
        this.userData = null;
        this.todosOsItens = []; 
        this.todosOsItensCarregados = false;
        this.usuarios = [];
        this.usuariosCarregados = false;
        this.historico = [];
        this.historicoCarregado = false;
        this.itensAdquiridosSet = new Set();
        
        this.carouselIndicators = null;
        this.itemModalElement = null;
        this.currentItemInModal = null;

        this.coresPadraoRaridade = {
            'comum': '#b0b0b0', 'common': '#b0b0b0',
            'incomum': '#60aa3a', 'uncommon': '#60aa3a',
            'raro': '#4ec1f3', 'rare': '#4ec1f3',
            'épico': '#bf6ee0', 'epic': '#bf6ee0',
            'lendário': '#e9a748', 'legendary': '#e9a748'
        };

        this.init();
    }

    async init() {
        Swal.fire({
            title: 'Carregando Loja...',
            html: 'Espere um pouco enquanto buscamos os itens para você!',
            allowOutsideClick: false, allowEscapeKey: false,
            didOpen: () => { Swal.showLoading(); },
            background: 'rgba(0, 0, 0, 0.9)', color: '#fff',
            customClass: { popup: 'border-neon' }
        });
        this.pegaElementos();
        await this.verificaUsuario();
        this.buscaItensDisponiveis();
    }

    pegaElementos() {
        this.cosmeticosGrid = document.getElementById('shop-grid'); 
        this.carrouselItems = document.getElementById('carouselItems');
        this.navItens = document.getElementById('nav-itens');
        this.carouselIndicators = document.querySelector('#featuredCarousel .carousel-indicators');
        this.carouselControlsContainer = document.getElementById('carousel-external-controls');

        if (!this.cosmeticosGrid || !this.carrouselItems || !this.navItens) {
            console.error('Elementos essenciais da "Loja" não encontrados');
            return;
        }

        this.perfilModal = document.getElementById('userProfileModal');
        this.itemModalElement = document.getElementById('itemModal');
        
        this.btnBuy = document.getElementById('btn-buy');
        if (this.btnBuy) this.btnBuy.addEventListener('click', () => this.handleCompraClick());
        
        this.btnDevolver = document.getElementById('btn-devolver');
        if (this.btnDevolver) this.btnDevolver.addEventListener('click', () => this.handleDevolucaoClick());
        
        // --- FILTROS ABA LOJA ---
        this.shopTypeFilter = document.getElementById('shop-typeFilter');
        this.shopRarityFilter = document.getElementById('shop-rarityFilter');
        this.shopSearchInput = document.getElementById('shop-searchInput');
        this.shopDateStart = document.getElementById('shop-dateStart');
        this.shopDateEnd = document.getElementById('shop-dateEnd');
        this.shopCheckNew = document.getElementById('shop-checkNew');
        this.shopCheckForSale = document.getElementById('shop-checkForSale');
        this.shopCheckPromo = document.getElementById('shop-checkPromo');
        this.shopClearBtn = document.getElementById('shop-clearFilters');

        [this.shopTypeFilter, this.shopRarityFilter, this.shopSearchInput, this.shopDateStart, this.shopDateEnd, this.shopCheckNew, this.shopCheckForSale, this.shopCheckPromo].forEach(input => {
            if(input) input.addEventListener((input.type === 'text' || input.type === 'date') ? 'input' : 'change', () => this.renderizaItens());
        });

        if(this.shopClearBtn) {
            this.shopClearBtn.addEventListener('click', () => this.limparFiltrosLoja());
        }
        
        // --- FILTROS ABA TODOS OS ITENS ---
        this.allItemsTab = document.getElementById('all-items-tab');
        this.allItemsGrid = document.getElementById('all-items-grid');
        this.allTypeFilter = document.getElementById('all-typeFilter');
        this.allRarityFilter = document.getElementById('all-rarityFilter');
        this.allSearchInput = document.getElementById('all-searchInput');
        this.allDateStart = document.getElementById('all-dateStart');
        this.allDateEnd = document.getElementById('all-dateEnd');
        this.allCheckNew = document.getElementById('all-checkNew');
        this.allCheckForSale = document.getElementById('all-checkForSale');
        this.allCheckPromo = document.getElementById('all-checkPromo');
        this.allClearBtn = document.getElementById('all-clearFilters');

        [this.allTypeFilter, this.allRarityFilter, this.allSearchInput, this.allDateStart, this.allDateEnd, this.allCheckNew, this.allCheckForSale, this.allCheckPromo].forEach(input => {
            if(input) {
                const evt = (input.type === 'text' || input.type === 'date') ? 'input' : 'change';
                if (input.type === 'text') {
                    let timeout;
                    input.addEventListener('input', () => { clearTimeout(timeout); timeout = setTimeout(() => this.buscaTodosOsItens(), 500); });
                } else {
                    input.addEventListener(evt, () => this.buscaTodosOsItens());
                }
            }
        });

        if(this.allClearBtn) {
            this.allClearBtn.addEventListener('click', () => this.limparFiltrosTodos());
        }

        if (this.allItemsTab) this.allItemsTab.addEventListener('show.bs.tab', () => { if (!this.todosOsItensCarregados) this.buscaTodosOsItens(); });

        // --- ABA MEUS ITENS ---
        this.myItemsTabContainer = document.getElementById('my-items-tab-container');
        this.myItemsTab = document.getElementById('my-items-tab');
        this.myItemsGrid = document.getElementById('my-items-grid');
        
        // Correção: Adiciona o listener corretamente
        if (this.myItemsTab) {
            this.myItemsTab.addEventListener('show.bs.tab', () => this.renderizarMeusItens());
        }

        // --- ABA USUÁRIOS ---
        this.usersTabContainer = document.getElementById('users-tab-container');
        this.usersTab = document.getElementById('users-tab');
        this.usersListContainer = document.getElementById('users-list');
        this.userSearchInput = document.getElementById('userSearchInput');
        this.userSortFilter = document.getElementById('userSortFilter');

        if (this.usersTab) this.usersTab.addEventListener('show.bs.tab', () => this.handleUsersTabFocus());
        
        // Modal Perfil
        this.historyTabButton = document.getElementById('profile-tab-history-tab');
        this.historyListContainer = document.getElementById('modal-user-history');
        this.itemsTabButton = document.getElementById('profile-tab-items-tab'); 
    }

    // --- MÉTODOS DE LIMPEZA ---
    limparFiltrosLoja() {
        if(this.shopSearchInput) this.shopSearchInput.value = '';
        if(this.shopTypeFilter) this.shopTypeFilter.value = '';
        if(this.shopRarityFilter) this.shopRarityFilter.value = '';
        if(this.shopDateStart) this.shopDateStart.value = '';
        if(this.shopDateEnd) this.shopDateEnd.value = '';
        if(this.shopCheckNew) this.shopCheckNew.checked = false;
        if(this.shopCheckForSale) this.shopCheckForSale.checked = false;
        if(this.shopCheckPromo) this.shopCheckPromo.checked = false;
        this.renderizaItens();
    }

    limparFiltrosTodos() {
        if(this.allSearchInput) this.allSearchInput.value = '';
        if(this.allTypeFilter) this.allTypeFilter.value = '';
        if(this.allRarityFilter) this.allRarityFilter.value = '';
        if(this.allDateStart) this.allDateStart.value = '';
        if(this.allDateEnd) this.allDateEnd.value = '';
        if(this.allCheckNew) this.allCheckNew.checked = false;
        if(this.allCheckForSale) this.allCheckForSale.checked = false;
        if(this.allCheckPromo) this.allCheckPromo.checked = false;
        this.buscaTodosOsItens();
    }

    // --- DADOS E AUTH ---

    async verificaUsuario() {
        if (this.user) {
            await this.buscaDadosusuario();
            if (this.myItemsTabContainer) this.myItemsTabContainer.style.display = 'block';
            if (this.usersTabContainer) this.usersTabContainer.style.display = 'block'; 
        } else {
            this.navItens.innerHTML = `<a href="/login.html" class="btn btn-login">Entrar</a><a href="/cadastro.html" class="btn btn-signup">Criar Conta</a>`;
            if (this.myItemsTabContainer) this.myItemsTabContainer.style.display = 'none';
            if (this.usersTabContainer) this.usersTabContainer.style.display = 'none';
        }
    }

    buscaDadosusuario() {
        return fetch(`${this.API_BASE_URL}/perfis/me`, {
            method: 'GET',
            headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${this.user}` }
        })
        .then(response => {
            if (!response.ok) { localStorageManager.removeToken(); throw new Error('Erro ao buscar dados do usuário'); }
            return response.json();
        })
        .then(data => {
            this.userData = data;
            if (data.itensAdquiridos && Array.isArray(data.itensAdquiridos)) {
                this.itensAdquiridosSet = new Set(data.itensAdquiridos.map(item => item.id));
            }
            this.atualizaNavUsuario(data);
            this.buscaHistoricoUsuario();
            
            // --- CORREÇÃO IMPORTANTE: Renderiza meus itens assim que os dados chegam ---
            if(this.myItemsGrid) {
                this.renderizarMeusItens();
            }
        })
        .catch(error => {
            this.user = null;
            localStorageManager.removeToken();
            this.navItens.innerHTML = `<a href="/login.html" class="btn btn-login">Entrar</a><a href="/cadastro.html" class="btn btn-signup">Criar Conta</a>`;
        });
    }

    atualizaNavUsuario(userData) {
        const creditosFormatados = (userData.creditos || 0).toLocaleString('pt-BR');
        this.navItens.innerHTML = `
            <span class="nav-creditos d-flex align-items-center me-3">${creditosFormatados} V-Bucks</span>
            <button type="button" class="btn btn-perfil" data-bs-toggle="modal" data-bs-target="#userProfileModal" id="nav-perfil-btn"><i class="bi bi-person-fill me-2"></i> Perfil</button>
            <button id="nav-logout" class="btn btn-logout"><i class="bi bi-box-arrow-right me-2"></i> Sair</button>
        `;
        document.getElementById('nav-logout')?.addEventListener('click', () => {
            localStorageManager.removeToken(); 
            this.user = null;
            window.location.href = 'index.html';
        });
        document.getElementById('nav-perfil-btn')?.addEventListener('click', () => this.preencherModalPerfil(this.userData));
    }

    // --- RENDERIZAR MEUS ITENS (TAB) ---

    renderizarMeusItens() {
        if (!this.myItemsGrid) return;
        this.myItemsGrid.innerHTML = ''; 
        
        // Garante que pegamos o array, mesmo se userData for nulo no inicio
        const itensAdquiridos = this.userData ? (this.userData.itensAdquiridos || []) : [];

        if (itensAdquiridos.length === 0) {
            this.myItemsGrid.innerHTML = this.gerarHTMLVazio('Você ainda não adquiriu nenhum item.');
            return;
        }

        // Normaliza os dados para o formato que a classe espera
        let itensProcessados = itensAdquiridos.map(itemData => {
            // Detecta se é bundle (pode vir como flag ou array de filhos)
            const isBundle = itemData.isBundle || (itemData.bundleItems && itemData.bundleItems.length > 0);
            
            return new ValidadorItem(
                itemData.id, itemData.nome, itemData.tipo, itemData.raridade,
                itemData.preco, itemData.urlImagem, itemData.descricao,
                itemData.isNew, itemData.isForSale, 
                true, // isAdquirido
                itemData.dataInclusao,
                itemData.cores, 
                isBundle, // Passa explicitamente se é bundle
                itemData.bundleItems
            ).validaDados();
        });

        // --- CHAMA COM TRUE para mostrar o Card Pai do Bundle ---
        const listaFinal = this.organizarItensComBundles(itensProcessados, true);
        
        const fragmento = document.createDocumentFragment();
        for (const item of listaFinal) {
            fragmento.appendChild(this.criarCard(item));
        }
        this.myItemsGrid.appendChild(fragmento);
    }

    // ... (O restante do código abaixo permanece o mesmo, com o mapa de tipos corrigido) ...

    preencherModalPerfil(userData) {
        if (!userData) return;
        const email = userData.email || 'Usuário';
        const nomeUsuario = userData.nome || email.split('@')[0];
        document.getElementById('modal-avatar').innerHTML = `<i class="bi bi-file-person"></i>`;
        document.getElementById('modal-user-name').textContent = nomeUsuario;
        document.getElementById('stat-items').textContent = userData.itensAdquiridos?.length || 0;
        document.getElementById('stat-vbucks').textContent = (userData.creditos || 0).toLocaleString('pt-BR');
        document.getElementById('modal-user-email').textContent = email;
        
        const itensAdquiridos = userData.itensAdquiridos || [];
        const itensContainer = document.getElementById('modal-user-items');
        
        if (itensAdquiridos.length > 0) {
            itensContainer.innerHTML = ''; 
            itensAdquiridos.forEach(item => {
                const imagem = item.urlImagem ? `<img src="${this.sanitizarUrl(item.urlImagem)}" alt="${item.nome}">` : '<i class="bi bi-question-lg"></i>';
                let styleBackground = '';
                let classeBackground = '';
                if (item.cores && Array.isArray(item.cores) && item.cores.length > 0) {
                    styleBackground = `style="${this.gerarEstiloBackground(item.cores)}"`;
                } else {
                    const classeRaridade = this.obterClasseRaridade(item.raridade || 'Comum');
                    classeBackground = `bg-rarity-${classeRaridade}`;
                }
                itensContainer.innerHTML += `
                    <div class="user-item-mini-card">
                        <div class="user-item-mini-image ${classeBackground}" ${styleBackground}>
                            ${imagem}
                        </div>
                        <div class="user-item-mini-name">${this.sanitizarTexto(item.nome || 'Item')}</div>
                    </div>`;
            });
        } else {
            itensContainer.innerHTML = `
                <div class="d-flex flex-column justify-content-center align-items-center w-100" style="grid-column: 1 / -1; min-height: 200px; text-align: center;">
                    <i class="bi bi-box-seam text-secondary mb-3" style="font-size: 3rem; opacity: 0.5;"></i>
                    <p class="text-light fs-5 m-0">Nenhum item adquirido.</p>
                </div>`;
        }
        
        const historyTabContainer = document.getElementById('modal-history-section'); 
        if (this.user && this.userData && userData.id === this.userData.id) {
            if (historyTabContainer) historyTabContainer.style.display = 'block';
            this.renderizarHistorico(); 
        } else {
            if (historyTabContainer) historyTabContainer.style.display = 'none';
            const itemsTabButton = document.getElementById('profile-tab-items-tab');
            if (itemsTabButton) bootstrap.Tab.getOrCreateInstance(itemsTabButton).show();
        }
    }

    async buscaItensDisponiveis() {
        const headers = { 'Content-Type': 'application/json' };
        if (this.user) headers['Authorization'] = `Bearer ${this.user}`;
        
        try {
            const response = await fetch(`${this.API_BASE_URL}/cosmeticos/loja`, { headers });
            if (!response.ok) throw new Error(`Erro HTTP: ${response.status}`);
            const itensDaApi = await response.json();
            if (Array.isArray(itensDaApi)) {
                this.itens = itensDaApi.map(item => {
                    const isAdquirido = this.itensAdquiridosSet.has(item.id);
                    return new ValidadorItem(
                        item.id, item.nome, item.tipo, item.raridade, item.preco, item.urlImagem, 
                        item.descricao, item.isNew, item.isForSale, isAdquirido, item.dataInclusao, 
                        item.cores, item.isBundle, item.bundleItems
                    ).validaDados();
                });
                this.renderizaItens(); 
                this.preencheCarrousel(); 
                Swal.close();
            } else { throw new Error("API /loja/todos não retornou um array."); }
        } catch (error) {
            console.error('Erro ao buscar itens:', error);
            Swal.fire({ icon: 'error', title: 'Ops!', text: 'Erro ao carregar itens.', background: 'rgba(0, 0, 0, 0.9)', color: '#fff' });
            this.mostrarErro('Erro ao carregar itens da loja.', this.cosmeticosGrid);
        }
    }

    hexToHSL(hex) {
        let result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})/i.exec(hex);
        if (!result) return { h: 0, s: 0, l: 0 };
        let r = parseInt(result[1], 16) / 255, g = parseInt(result[2], 16) / 255, b = parseInt(result[3], 16) / 255;
        let max = Math.max(r, g, b), min = Math.min(r, g, b);
        let h, s, l = (max + min) / 2;
        if (max === min) { h = s = 0; } 
        else {
            let d = max - min;
            s = l > 0.5 ? d / (2 - max - min) : d / (max + min);
            switch (max) {
                case r: h = (g - b) / d + (g < b ? 6 : 0); break;
                case g: h = (b - r) / d + 2; break;
                case b: h = (r - g) / d + 4; break;
            }
            h /= 6;
        }
        return { h: h * 360, s: s, l: l };
    }

    obterCorOrdenacao(item) {
        if (item.cores && Array.isArray(item.cores) && item.cores.length > 0) {
            let hex = item.cores[0];
            if (hex.length > 7) hex = hex.substring(0, 7);
            return hex;
        }
        const raridadeChave = (item.raridade || 'comum').toLowerCase();
        for (const [key, color] of Object.entries(this.coresPadraoRaridade)) {
            if (raridadeChave.includes(key)) return color;
        }
        return '#808080';
    }

    ordenarPorCor(lista) {
        return lista.sort((a, b) => {
            const hexA = this.obterCorOrdenacao(a);
            const hexB = this.obterCorOrdenacao(b);
            const hslA = this.hexToHSL(hexA);
            const hslB = this.hexToHSL(hexB);
            const isGrayscaleA = hslA.s < 0.10; 
            const isGrayscaleB = hslB.s < 0.10;

            if (isGrayscaleA && !isGrayscaleB) return 1;
            if (!isGrayscaleA && isGrayscaleB) return -1;
            if (isGrayscaleA && isGrayscaleB) return hslB.l - hslA.l;
            if (hslA.h !== hslB.h) return hslB.h - hslA.h;
            return hslB.l - hslA.l;
        });
    }

    organizarItensComBundles(listaTotal, mostrarBundlesPais = false) {
        const processadosIds = new Set();
        const listaFinal = [];
        const mapaItens = new Map(listaTotal.map(i => [i.id, i]));

        let bundles = listaTotal.filter(item => item.isBundle);
        bundles = this.ordenarPorCor(bundles);

        bundles.forEach(bundle => {
            if (mostrarBundlesPais) {
                listaFinal.push(bundle);
                processadosIds.add(bundle.id);
            }

            if (bundle.bundleItems && Array.isArray(bundle.bundleItems) && bundle.bundleItems.length > 0) {
                let itensDoBundle = [];
                bundle.bundleItems.forEach(itemId => {
                    if (mapaItens.has(itemId)) itensDoBundle.push(mapaItens.get(itemId));
                });
                itensDoBundle = this.ordenarPorCor(itensDoBundle);
                itensDoBundle.forEach(itemFilho => {
                    if (!processadosIds.has(itemFilho.id)) {
                        listaFinal.push(itemFilho);
                        processadosIds.add(itemFilho.id);
                    }
                });
            }
        });

        let itensSobraram = listaTotal.filter(item => {
            if (!mostrarBundlesPais && item.isBundle) return false;
            return !processadosIds.has(item.id);
        });

        if (itensSobraram.length > 0) {
            const sobraOrdenada = this.ordenarPorCor(itensSobraram);
            listaFinal.push(...sobraOrdenada);
        }

        return listaFinal;
    }

    renderizaItens() {
        if (!this.cosmeticosGrid) return;
        
        const mapaTipos = {
            'outfit': 'traje', 'skin': 'traje',
            'backpack': 'acessório para as costas', 'mochila': 'acessório para as costas',
            'pickaxe': 'picareta', 'glider': 'asa-delta',
            'emote': 'gesto', 'wrap': 'envelopamento', 'pet': 'mascote',
            'shoes': 'sapatos', 'sapatos': 'sapatos'
        };

        let tipoSelecionado = this.shopTypeFilter ? this.shopTypeFilter.value.toLowerCase() : '';
        if (mapaTipos[tipoSelecionado]) {
            tipoSelecionado = mapaTipos[tipoSelecionado];
        }

        const raridadeValue = this.shopRarityFilter ? this.shopRarityFilter.value : '';
        const busca = this.shopSearchInput ? this.shopSearchInput.value.toLowerCase().trim() : '';
        const dataInicio = this.shopDateStart && this.shopDateStart.value ? new Date(this.shopDateStart.value) : null;
        const dataFim = this.shopDateEnd && this.shopDateEnd.value ? new Date(this.shopDateEnd.value) : null;
        const apenasNovos = this.shopCheckNew ? this.shopCheckNew.checked : false;
        const apenasVenda = this.shopCheckForSale ? this.shopCheckForSale.checked : false;
        const apenasPromo = this.shopCheckPromo ? this.shopCheckPromo.checked : false;

        let itensFiltrados = this.itens.filter(item => {
            const matchTipo = !tipoSelecionado || (item.tipo && item.tipo.toLowerCase().includes(tipoSelecionado));
            const classeRaridade = this.obterClasseRaridade(item.raridade);
            const matchRaridade = !raridadeValue || classeRaridade === raridadeValue;
            const matchBusca = !busca || (item.nome && item.nome.toLowerCase().includes(busca));

            if (apenasNovos && !item.isNew) return false;
            if (apenasVenda && (!item.isForSale || item.isAdquirido)) return false;
            if (apenasPromo && !item.isForSale) return false;

            if (dataInicio || dataFim) {
                const dataItem = new Date(item.dataInclusao);
                if (dataInicio && dataItem < dataInicio) return false;
                if (dataFim) {
                    const fimDoDia = new Date(dataFim);
                    fimDoDia.setHours(23, 59, 59, 999);
                    if (dataItem > fimDoDia) return false;
                }
            }
            return matchTipo && matchRaridade && matchBusca;
        });
        
        this.cosmeticosGrid.innerHTML = '';
        if (itensFiltrados.length === 0) {
           this.cosmeticosGrid.innerHTML = this.gerarHTMLVazio('Nenhum item encontrado com estes filtros.');
           return;
        }

        const listaFinalParaExibir = this.organizarItensComBundles(itensFiltrados, false);
        const fragmento = document.createDocumentFragment();
        for (const item of listaFinalParaExibir) {
            fragmento.appendChild(this.criarCard(item));
        }
        this.cosmeticosGrid.appendChild(fragmento);
    }

    async handleAllItemsTabFocus() {
        if (!this.todosOsItensCarregados) {
            await this.buscaTodosOsItens();
            this.todosOsItensCarregados = true;
        }
    }

    async buscaTodosOsItens() {
        if (!this.allItemsGrid) return;
        const headers = { 'Content-Type': 'application/json' };
        if (this.user) headers['Authorization'] = `Bearer ${this.user}`;

        let baseUrl = `${this.API_BASE_URL}/cosmeticos`;
        if (this.allCheckNew && this.allCheckNew.checked) {
            baseUrl = `${this.API_BASE_URL}/cosmeticos/novos`;
        } else if (this.allCheckForSale && this.allCheckForSale.checked) {
            baseUrl = `${this.API_BASE_URL}/cosmeticos/loja`;
        }

        const url = new URL(baseUrl);

        const mapaTiposAPI = {
            'outfit': 'Traje', 'skin': 'Traje', 'traje': 'Traje',
            'backpack': 'Acessório para as Costas', 'mochila': 'Acessório para as Costas', 'acessório para as costas': 'Acessório para as Costas',
            'pickaxe': 'Picareta', 'picareta': 'Picareta',
            'glider': 'Asa-delta', 'asa-delta': 'Asa-delta',
            'emote': 'Gesto', 'gesto': 'Gesto',
            'wrap': 'Envelopamento', 'envelopamento': 'Envelopamento',
            'pet': 'Mascote', 'mascote': 'Mascote',
            'shoes': 'Sapatos', 'sapatos': 'Sapatos'
        };
        
        let tipoRaw = this.allTypeFilter ? this.allTypeFilter.value.toLowerCase() : '';
        let tipoAPI = mapaTiposAPI[tipoRaw] || (this.allTypeFilter ? this.allTypeFilter.value : '');

        const raridadeRaw = this.allRarityFilter ? this.allRarityFilter.value : '';
        const busca = this.allSearchInput ? this.allSearchInput.value.trim() : '';
        const mapaRaridade = { 'serie': 'Série', 'legendary': 'Lendário', 'epic': 'Épico', 'rare': 'Raro', 'uncommon': 'Incomum', 'common': 'Comum' };
        const raridadeAPI = mapaRaridade[raridadeRaw] || '';

        if (busca) url.searchParams.append('nome', busca);
        if (tipoAPI) url.searchParams.append('tipo', tipoAPI);
        if (raridadeAPI) url.searchParams.append('raridade', raridadeAPI);
        
        url.searchParams.append('page', '0'); 
        url.searchParams.append('size', '100');
        url.searchParams.append('sort', 'dataInclusao,desc'); 

        try {
            this.allItemsGrid.innerHTML = `<div class="d-flex flex-column justify-content-center align-items-center w-100" style="grid-column: 1 / -1; min-height: 400px; color: var(--text-secondary);"><div class="spinner-border text-primary mb-3" style="width: 3rem; height: 3rem; border-width: 4px;" role="status"></div><p class="fs-5 fw-bold text-light" style="letter-spacing: 1px; animation: pulse 1.5s infinite;">BUSCANDO NA API...</p></div>`;
            const response = await fetch(url.toString(), { headers }); 
            if (!response.ok) throw new Error(`Erro HTTP: ${response.status}`);
            
            const dadosDaPagina = await response.json();
            const listaItensRaw = dadosDaPagina.content || (Array.isArray(dadosDaPagina) ? dadosDaPagina : []);

            let listaProcessada = listaItensRaw.map(item => {
                const isAdquirido = this.itensAdquiridosSet.has(item.id);
                return new ValidadorItem(
                    item.id, item.nome, item.tipo, item.raridade, item.preco, item.urlImagem, 
                    item.descricao, item.isNew, item.isForSale, isAdquirido, item.dataInclusao,
                    item.cores, item.isBundle, item.bundleItems
                ).validaDados();
            });

            listaProcessada = this.filtrarListaLocalmente(listaProcessada);
            this.todosOsItens = listaProcessada;
            this.renderizarTodosOsItens(); 
        } catch (error) {
            console.error('Erro ao buscar itens:', error);
            this.mostrarErro('Erro ao carregar itens.', this.allItemsGrid);
        }
    }

    filtrarListaLocalmente(lista) {
        const dataInicio = this.allDateStart && this.allDateStart.value ? new Date(this.allDateStart.value) : null;
        const dataFim = this.allDateEnd && this.allDateEnd.value ? new Date(this.allDateEnd.value) : null;
        const apenasPromo = this.allCheckPromo ? this.allCheckPromo.checked : false;

        return lista.filter(item => {
            if (dataInicio || dataFim) {
                const dataItem = new Date(item.dataInclusao);
                if (dataInicio && dataItem < dataInicio) return false;
                if (dataFim) {
                    const fimDoDia = new Date(dataFim);
                    fimDoDia.setHours(23, 59, 59, 999);
                    if (dataItem > fimDoDia) return false;
                }
            }
            if (apenasPromo && !item.isForSale) return false;
            return true;
        });
    }
    
    renderizarTodosOsItens() {
        if (!this.allItemsGrid) return;
        this.allItemsGrid.innerHTML = '';
        
        if (this.todosOsItens.length === 0) {
            this.allItemsGrid.innerHTML = this.gerarHTMLVazio('Nenhum item encontrado.');
            return;
        }

        const listaFinal = this.organizarItensComBundles(this.todosOsItens, false);
        const fragmento = document.createDocumentFragment();
        for (const item of listaFinal) {
            fragmento.appendChild(this.criarCard(item));
        }
        this.allItemsGrid.appendChild(fragmento);
    }

    gerarHTMLVazio(mensagem) {
        return `<div class="col-12 w-100 d-flex flex-column justify-content-center align-items-center" style="grid-column: 1 / -1; min-height: 200px;"><i class="bi bi-search text-secondary mb-3" style="font-size: 2rem; opacity: 0.5;"></i><p class="text-center text-light fs-5 m-0">${mensagem}</p></div>`;
    }

    async handleUsersTabFocus() {
        if (this.usuariosCarregados) return; 
        if (!this.user) { this.mostrarErro('Você precisa estar logado para ver os usuários.', this.usersListContainer); return; }
        await this.buscaUsuarios();
        if (this.userSearchInput) this.userSearchInput.addEventListener('input', () => this.renderizarUsuarios());
        if (this.userSortFilter) this.userSortFilter.addEventListener('change', () => this.renderizarUsuarios());
    }

    async buscaUsuarios() {
        if (!this.usersListContainer) return;
        try {
            this.usersListContainer.innerHTML = '<div class="col-12"><p class="text-center text-light fs-5">Carregando usuários...</p></div>';
            const response = await fetch(`${this.API_BASE_URL}/perfis`, { headers: { 'Authorization': `Bearer ${this.user}` } }); 
            if (!response.ok) throw new Error(`Erro HTTP: ${response.status}`);
            const dadosDaPagina = await response.json();
            if (dadosDaPagina.content && Array.isArray(dadosDaPagina.content)) {
                this.usuarios = dadosDaPagina.content;
                this.usuariosCarregados = true;
                this.renderizarUsuarios();
            } else { throw new Error("A API /perfis não retornou um objeto com a propriedade 'content'."); }
        } catch (error) { this.mostrarErro(error.message, this.usersListContainer); }
    }

    renderizarUsuarios() {
        if (!this.usersListContainer) return;
        const busca = this.userSearchInput.value.toLowerCase();
        const sortBy = this.userSortFilter.value;
        let usuariosFiltrados = this.usuarios.filter(user => !busca || user.email.toLowerCase().includes(busca));

        usuariosFiltrados.sort((a, b) => {
            if (sortBy === 'email') return a.email.localeCompare(b.email);
            if (sortBy === 'name') {
                const nomeA = a.nome || a.email;
                const nomeB = b.nome || b.email;
                return nomeA.localeCompare(nomeB);
            }
            return a.id - b.id;
        });

        this.usersListContainer.innerHTML = '';
        if (usuariosFiltrados.length === 0) { this.usersListContainer.innerHTML = '<p class="text-center text-light fs-5">Nenhum usuário encontrado.</p>'; return; }

        this.usersListContainer.innerHTML = `
            <div class="table-responsive">
                <table class="table table-dark table-striped table-hover align-middle text-nowrap custom-mobile-table">
                    <thead><tr><th scope="col">Email</th><th scope="col">V-Bucks</th><th scope="col" class="text-end">Ação</th></tr></thead>
                    <tbody>
                        ${usuariosFiltrados.map(user => `
                            <tr>
                                <td data-label="Email">${this.sanitizarTexto(user.nome)}</td>
                                <td data-label="V-Bucks" class="fw-bold">${user.creditos.toLocaleString('pt-BR')}</td>
                                <td data-label="Ação" class="text-end"><button class="btn btn-sm btn-info btn-visualizar-usuario" data-userid="${user.id}"><i class="bi bi-eye-fill"></i> <span class="d-none d-sm-inline">Visualizar</span></button></td>
                            </tr>`).join('')}
                    </tbody>
                </table>
            </div>`;
        this.usersListContainer.querySelectorAll('.btn-visualizar-usuario').forEach(button => {
            button.addEventListener('click', (e) => this.handleVisualizarUsuarioClick(e));
        });
    }

    handleVisualizarUsuarioClick(event) {
        const userId = event.currentTarget.dataset.userid;
        if (!userId) return;
        this.buscaDadosUsuarioPorId(userId);
    }

    async buscaDadosUsuarioPorId(id) {
        Swal.fire({ title: 'Buscando usuário...', allowOutsideClick: false, didOpen: () => Swal.showLoading() });
        try {
            const response = await fetch(`${this.API_BASE_URL}/perfis/${id}`, { headers: { 'Authorization': `Bearer ${this.user}` } });
            if (!response.ok) throw new Error('Não foi possível buscar os dados do usuário.');
            const dadosUsuario = await response.json();
            Swal.close();
            this.preencherModalPerfil(dadosUsuario);
            const modal = bootstrap.Modal.getOrCreateInstance(this.perfilModal);
            modal.show();
        } catch (error) { Swal.fire({ title: 'Erro', text: error.message, icon: 'error' }); }
    }

    async buscaHistoricoUsuario() {
        if (this.historicoCarregado || !this.user) return;
        try {
            const response = await fetch(`${this.API_BASE_URL}/perfis/me/historico`, { headers: { 'Authorization': `Bearer ${this.user}` } });
            if (!response.ok) throw new Error('Não foi possível carregar o histórico.');
            const dadosPagina = await response.json();
            if (dadosPagina.content && Array.isArray(dadosPagina.content)) {
                this.historico = dadosPagina.content;
                this.historicoCarregado = true;
            }
        } catch (error) { if (this.historyListContainer) this.historyListContainer.innerHTML = `<p class="text-center text-danger">Não foi possível carregar o histórico.</p>`; }
    }

    renderizarHistorico() {
        if (!this.historyListContainer) return;
        this.historyListContainer.innerHTML = ''; 
        if (this.historico.length === 0) { this.historyListContainer.innerHTML = '<p class="text-center text-light">Nenhuma transação encontrada.</p>'; return; }
        this.historico.forEach(item => {
            const isCompra = item.tipo === 'COMPRA';
            const tipoClasse = isCompra ? 'tipo-compra' : 'tipo-devolucao';
            const iconClasse = isCompra ? 'bi-cart-dash-fill' : 'bi-arrow-counterclockwise';
            const valorPrefixo = isCompra ? '-' : '+';
            this.historyListContainer.innerHTML += `
                <div class="history-item ${tipoClasse}">
                    <div class="history-item-icon"><i class="bi ${iconClasse}"></i></div>
                    <div class="history-item-details"><div class="history-item-title">${this.sanitizarTexto(item.cosmeticoNome)}</div><div class="history-item-date">${this.formatarData(item.dataTransacao)}</div></div>
                    <div class="history-item-value">${valorPrefixo}${item.valor.toLocaleString('pt-BR')}</div>
                </div>`;
        });
    }

    preencheCarrousel() {
        if (!this.carrouselItems) return;
        const bundles = this.itens.filter(item => item.isBundle === true && !item.isAdquirido);
        this.carrouselItems.innerHTML = ''; 
        if (this.carouselIndicators) this.carouselIndicators.innerHTML = ''; 

        if (bundles.length === 0) {
            this.alternarControlesCarrossel(false);
            this.carrouselItems.innerHTML = `<div class="carousel-item active" style="height: 600px;"><div class="d-flex flex-column justify-content-center align-items-center h-100 w-100 bg-dark"><i class="bi bi-bag-check mb-3" style="font-size: 4rem; color: rgba(255,255,255,0.2);"></i><h4 class="text-white-50">Todos os pacotões adquiridos ou indisponíveis</h4></div></div>`;
            return;
        }

        this.alternarControlesCarrossel(true);
        const fragmentoItens = document.createDocumentFragment();
        const fragmentoIndicadores = document.createDocumentFragment();

        bundles.forEach((item, index) => {
            fragmentoItens.appendChild(this.criarItemCarrousel(item, index === 0));
            if (this.carouselIndicators) {
                const btnIndicator = document.createElement('button');
                btnIndicator.type = 'button';
                btnIndicator.dataset.bsTarget = '#featuredCarousel';
                btnIndicator.dataset.bsSlideTo = index;
                btnIndicator.ariaLabel = `Slide ${index + 1}`;
                if (index === 0) { btnIndicator.classList.add('active'); btnIndicator.ariaCurrent = 'true'; }
                fragmentoIndicadores.appendChild(btnIndicator);
            }
        });

        this.carrouselItems.appendChild(fragmentoItens);
        if (this.carouselIndicators) this.carouselIndicators.appendChild(fragmentoIndicadores);
    }

    alternarControlesCarrossel(mostrar) {
        const display = mostrar ? 'flex' : 'none';
        if (this.carouselControlsContainer) this.carouselControlsContainer.style.display = display;
        if (this.carouselIndicators) this.carouselIndicators.style.display = display;
    }

    criarItemCarrousel(item, isActive) {
        const div = document.createElement('div');
        div.className = `carousel-item ${isActive ? 'active' : ''}`;
        div.style.height = '600px'; 
        const imagemUrl = item.urlImagem ? this.sanitizarUrl(item.urlImagem) : '';
        let estiloBackground = this.gerarEstiloBackground(item.cores, false);
        let classeRaridade = !estiloBackground ? `bg-rarity-${this.obterClasseRaridade(item.raridade)}` : '';
        estiloBackground = estiloBackground.replace('!important', '').replace('!important', '');

        div.innerHTML = `
            <div class="${classeRaridade}" style="position: absolute; top: 0; left: 0; width: 100%; height: 100%; ${estiloBackground}; z-index: 1;"></div>
            <div style="position: absolute; top: 0; left: 0; width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; z-index: 2;">
                <img src="${imagemUrl}" alt="${this.sanitizarTexto(item.nome)}" style="max-width: 100%; max-height: 100%; width: auto; height: 100%; object-fit: contain; filter: drop-shadow(0 10px 20px rgba(0,0,0,0.5));">
            </div>
            <div style="position: absolute; bottom: 0; left: 0; width: 100%; height: 60%; z-index: 3;"></div>
            <div class="carousel-caption d-flex flex-column flex-md-row justify-content-between align-items-end w-100 p-4 p-md-5" style="z-index: 4; bottom: 0; left: 0; right: 0; text-align: left; padding-bottom: 50px !important;">
                <div style="max-width: 65%;">
                    <span class="badge bg-warning text-dark mb-2 shadow-sm" style="font-size: 0.9rem;">PACOTÃO EXCLUSIVO</span>
                    <h1 class="fw-bold text-white text-uppercase mb-2" style="text-shadow: 2px 2px 10px rgba(0,0,0,1); font-size: 3rem; line-height: 1;">${this.sanitizarTexto(item.nome || 'Sem nome')}</h1>
                    <p class="text-white-50 mb-3 d-none d-md-block" style="font-size: 1.1rem; text-shadow: 1px 1px 5px rgba(0,0,0,1); max-width: 80%;">${this.sanitizarTexto(item.descricao || '')}</p>
                    <h2 class="m-0 text-warning fw-bold" style="text-shadow: 1px 1px 5px rgba(0,0,0,1); font-size: 2rem;">${item.preco ? `${item.preco.toLocaleString('pt-BR')} V-Bucks` : 'Indisponível'}</h2>
                </div>
                <div class="d-flex gap-3 mt-4 mt-md-0">
                    <button class="btn btn-light btn-lg px-4 py-3 fw-bold btn-carousel-buy shadow-lg" style="border-radius: 50px; white-space: nowrap; min-width: 160px;"><i class="bi bi-cart-fill me-2"></i>Comprar</button>
                    <button class="btn btn-outline-light btn-lg px-4 py-3 fw-bold btn-carousel-view shadow-lg" style="border-radius: 50px; backdrop-filter: blur(4px); white-space: nowrap; min-width: 160px;"><i class="bi bi-eye me-2"></i>Detalhes</button>
                </div>
            </div>`;
        div.querySelector('.btn-carousel-buy')?.addEventListener('click', () => { this.currentItemInModal = item; this.handleCompraClick(); });
        div.querySelector('.btn-carousel-view')?.addEventListener('click', () => this.abrirModalItem(item));
        return div;
    }

    gerarEstiloBackground(cores, retornarHexMaisEscuro = false) {
        if (!cores || !Array.isArray(cores) || cores.length === 0) return retornarHexMaisEscuro ? '#000000' : '';
        const getLuminosidade = (hex) => {
            const c = hex.replace('#', '').substring(0, 6);
            return (parseInt(c.substr(0, 2), 16) * 299 + parseInt(c.substr(2, 2), 16) * 587 + parseInt(c.substr(4, 2), 16) * 114) / 1000;
        };
        let coresFormatadas = cores.map(c => {
            let hex = c.startsWith('#') ? c : `#${c}`;
            if (hex.length === 9) hex = hex.substring(0, 7); 
            return hex;
        });
        if (coresFormatadas.length > 1) coresFormatadas.sort((a, b) => getLuminosidade(b) - getLuminosidade(a));
        if (retornarHexMaisEscuro) return coresFormatadas[coresFormatadas.length - 1];
        if (coresFormatadas.length > 1) return `background: linear-gradient(180deg, ${coresFormatadas.join(', ')}) !important;`;
        return `background: ${coresFormatadas[0]} !important; background-color: ${coresFormatadas[0]} !important;`;
    }
    
    obterClasseRaridade(raridade) {
        if (!raridade) return 'common';
        const r = raridade.toLowerCase().trim();
        if (r.includes('icon') || r.includes('ícones') || r.includes('icones')) return 'icon';
        if (r.includes('dc')) return 'dc';
        if (r.includes('marvel')) return 'marvel';
        if (r.includes('shadow') || r.includes('sombra')) return 'shadow';
        if (r.includes('frozen') || r.includes('congelada')) return 'frozen';
        if (r.includes('lava')) return 'lava';
        if (r.includes('dark') || r.includes('obscura')) return 'dark';
        if (r.includes('star wars')) return 'starwars';
        if (r.includes('gaming') || r.includes('jogos')) return 'gaming';
        if (r.includes('slurp') || r.includes('glup')) return 'slurp';
        if (r.includes('série') || r.includes('serie') || r.includes('series')) return 'serie';
        if (r.includes('lendário') || r.includes('legendary')) return 'legendary';
        if (r.includes('épico') || r.includes('epic')) return 'epic';
        if (r.includes('raro') || r.includes('rare')) return 'rare';
        if (r.includes('incomum') || r.includes('uncommon')) return 'uncommon';
        return 'common';
    }

    criarCard(item) {
        const card = document.createElement('div');
        card.className = 'col';
        const imagem = item.urlImagem ? `<img src="${this.sanitizarUrl(item.urlImagem)}" alt="${this.sanitizarTexto(item.nome || 'Item')}" />` : '<div class="placeholder-image">Sem imagem</div>';
        let classeRaridade = this.obterClasseRaridade(item.raridade);
        let styleBackground = item.cores && item.cores.length > 0 ? `style="${this.gerarEstiloBackground(item.cores)}"` : '';
        let classeBackground = item.cores && item.cores.length > 0 ? '' : `bg-rarity-${classeRaridade}`;
        
        const newBadge = item.isNew ? `<span class="badge status-badge badge-new">Novo</span>` : '';
        const forSaleBadge = (item.isForSale && !item.isAdquirido) ? `<span class="badge status-badge badge-for-sale">À Venda</span>` : '';
        const adquiridoBadge = (this.user && item.isAdquirido) ? `<span class="badge status-badge badge-adquirido">Adquirido</span>` : '';
        const bundleBadge = item.isBundle ? `<span class="badge bg-primary mb-1 me-1">Pacotão</span>` : '';

        card.innerHTML = `
            <div class="product-card">
                <div class="product-image ${classeBackground}" ${styleBackground}>${imagem}</div>
                <div class="card-body">
                    <div class="product-status-badges mb-2">${bundleBadge} ${newBadge} ${forSaleBadge} ${adquiridoBadge}</div>
                    <h5 class="product-name">${this.sanitizarTexto(item.nome || 'Sem nome')}</h5>
                    <p class="product-type">${this.sanitizarTexto(item.tipo || 'Cosmético')}</p>
                    <div class="product-price mt-2">${item.preco ? `${item.preco} V-Bucks` : 'Item indisponível'}</div>
                </div>
            </div>`;
        card.querySelector('.product-card')?.addEventListener('click', () => this.abrirModalItem(item));
        return card;
    }

    abrirModalItem(item) {
        if (!this.itemModalElement) return;
        this.preencherModalItem(item);
        const modal = bootstrap.Modal.getOrCreateInstance(this.itemModalElement);
        modal.show();
    }

    preencherModalItem(item) {
        this.currentItemInModal = item;
        const modal = this.itemModalElement;
        if (!modal) return;
        const imgContainer = modal.querySelector('#modal-item-image');
        if (imgContainer) {
            imgContainer.className = 'modal-image-container'; 
            imgContainer.removeAttribute('style');
            if (item.cores && Array.isArray(item.cores) && item.cores.length > 0) {
                imgContainer.setAttribute('style', this.gerarEstiloBackground(item.cores));
            } else {
                imgContainer.classList.add(`bg-rarity-${this.obterClasseRaridade(item.raridade)}`);
            }
            imgContainer.innerHTML = item.urlImagem ? `<img src="${this.sanitizarUrl(item.urlImagem)}" alt="${this.sanitizarTexto(item.nome)}" style="width: 100%; height: 100%; object-fit: contain; display: block;" />` : '<div class="placeholder-image">🎮</div>';
        }

        const badgeNew = modal.querySelector('#modal-item-badge-new');
        if (badgeNew) badgeNew.style.display = item.isNew ? 'inline-block' : 'none';
        const badgeSale = modal.querySelector('#modal-item-badge-sale');
        if (badgeSale) badgeSale.style.display = (item.isForSale && !item.isAdquirido) ? 'inline-block' : 'none';
        const rarityBadge = modal.querySelector('#modal-item-rarity');
        if (rarityBadge) {
            rarityBadge.textContent = this.sanitizarTexto(item.raridade);
            rarityBadge.className = `badge rarity-${this.obterClasseRaridade(item.raridade)}`;
        }

        const availability = modal.querySelector('#modal-item-availability');
        const btnBuy = modal.querySelector('#btn-buy'); 
        const btnDevolver = modal.querySelector('#btn-devolver');
        const title = modal.querySelector('#modal-availability-title');
        const text = modal.querySelector('#modal-availability-text');
        let icon = availability?.querySelector('i');

        if (availability && title && text) {
            if (this.user && item.isAdquirido) {
                title.textContent = 'Adquirido'; text.textContent = 'Este item já está na sua coleção.';
                if(icon) icon.className = 'bi bi-check-all'; availability.className = 'item-availability status-adquirido';
                if (btnBuy) btnBuy.style.display = 'none'; if (btnDevolver) btnDevolver.style.display = 'flex'; 
            } else if (item.isForSale) {
                title.textContent = 'Disponível na Loja'; text.textContent = 'Este item está disponível para compra agora.';
                if(icon) icon.className = 'bi bi-check-circle-fill'; availability.className = 'item-availability status-disponivel';
                if (btnBuy) btnBuy.style.display = 'flex'; if (btnDevolver) btnDevolver.style.display = 'none';
            } else {
                title.textContent = 'Indisponível'; text.textContent = 'Este item não está disponível para compra.';
                if(icon) icon.className = 'bi bi-x-circle-fill'; availability.className = 'item-availability status-indisponivel';
                if (btnBuy) btnBuy.style.display = 'none'; if (btnDevolver) btnDevolver.style.display = 'none';
            }
        }

        const itemName = modal.querySelector('#modal-item-name'); if (itemName) itemName.textContent = this.sanitizarTexto(item.nome);
        const itemType = modal.querySelector('#modal-item-type'); if (itemType) itemType.textContent = this.sanitizarTexto(item.tipo);
        const priceElement = modal.querySelector('#modal-item-price');
        if (priceElement) priceElement.textContent = item.preco !== null ? item.preco.toLocaleString('pt-BR') : 'N/A';
        const itemDesc = modal.querySelector('#modal-item-description'); if (itemDesc) itemDesc.textContent = this.sanitizarTexto(item.descricao || 'Sem descrição.');
        const detailRarity = modal.querySelector('#modal-detail-rarity'); if (detailRarity) detailRarity.textContent = this.sanitizarTexto(item.raridade);
        const detailCategory = modal.querySelector('#modal-detail-category'); if (detailCategory) detailCategory.textContent = this.sanitizarTexto(item.tipo);
        const detailDate = modal.querySelector('#modal-detail-date'); if (detailDate) detailDate.textContent = this.formatarData(item.dataInclusao);
    }

    handleCompraClick() {
        if (!this.user) { Swal.fire({ icon: 'error', title: 'Login Necessário', text: 'Você precisa estar logado para fazer uma compra.' }); return; }
        if (!this.currentItemInModal || !this.currentItemInModal.id) { Swal.fire({ icon: 'error', title: 'Erro', text: 'Nenhum item selecionado.' }); return; }
        if (this.userData.creditos < this.currentItemInModal.preco) { Swal.fire({ icon: 'warning', title: 'Saldo Insuficiente', text: `Você não tem V-Bucks suficientes.` }); return; }
        
        let itensJaPossuidos = [];
        if (this.currentItemInModal.isBundle && this.currentItemInModal.bundleItems && this.currentItemInModal.bundleItems.length > 0) {
            this.currentItemInModal.bundleItems.forEach(itemId => {
                if (this.itensAdquiridosSet.has(itemId)) {
                    const itemObj = this.buscarItemGlobalmente(itemId);
                    itensJaPossuidos.push(itemObj ? itemObj.nome : itemId);
                }
            });
        }

        let tituloModal = 'Confirmar Compra?';
        let htmlModal = `Você está prestes a comprar <b>${this.currentItemInModal.nome}</b> por <b>${this.currentItemInModal.preco.toLocaleString('pt-BR')} V-Bucks</b>.`;
        let iconModal = 'question';
        let confirmBtnText = 'Confirmar';

        if (itensJaPossuidos.length > 0) {
            tituloModal = 'Itens Duplicados Detectados';
            iconModal = 'warning';
            confirmBtnText = 'Comprar Mesmo Assim';
            const listaItensHtml = itensJaPossuidos.map(nome => `<li class="text-warning">${nome}</li>`).join('');
            htmlModal = `
                <div class="text-start">
                    <p>Você já possui os seguintes itens deste pacote:</p>
                    <ul style="list-style-type: disc; padding-left: 20px; margin-bottom: 15px;">${listaItensHtml}</ul>
                    <p class="mb-0">Você pagará o valor total de <b>${this.currentItemInModal.preco.toLocaleString('pt-BR')} V-Bucks</b> pelo restante do pacote.</p>
                    <small class="text-muted">O preço não é descontado automaticamente nesta transação.</small>
                </div>`;
        }

        Swal.fire({
            title: tituloModal, html: htmlModal, icon: iconModal,
            showCancelButton: true, confirmButtonText: confirmBtnText, cancelButtonText: 'Cancelar',
            background: 'rgba(0, 0, 0, 0.9)', color: '#fff', customClass: { popup: 'border-neon' }
        }).then((result) => { if (result.isConfirmed) this.executarCompra(this.currentItemInModal.id); });
    }

    async executarCompra(itemId) {
        try {
            const response = await fetch(`${this.API_BASE_URL}/compra/${itemId}`, { method: 'POST', headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${this.user}` } });
            if (!response.ok) throw new Error(await response.text() || 'Falha na transação.');
            await Swal.fire({ title: 'Compra Realizada!', text: `O item "${this.currentItemInModal.nome}" foi adicionado à sua coleção.`, icon: 'success' });
            await this.buscaDadosusuario(); 
            this.atualizarStatusItemLocal(itemId, true); 
            this.preencherModalItem(this.currentItemInModal);
            this.renderizaItens(); this.renderizarTodosOsItens(); this.renderizarMeusItens(); this.preencheCarrousel();
        } catch (error) { console.error('Erro ao comprar item:', error); Swal.fire({ title: 'Erro na Compra', text: error.message, icon: 'error' }); }
    }

    handleDevolucaoClick() {
        if (!this.user) { Swal.fire({ icon: 'error', title: 'Erro', text: 'Você precisa estar logado.' }); return; }
        if (!this.currentItemInModal || !this.currentItemInModal.id) return;
        Swal.fire({
            title: 'Confirmar Devolução?', html: `Você está prestes a devolver <b>${this.currentItemInModal.nome}</b>. Esta ação não pode ser desfeita.`,
            icon: 'warning', showCancelButton: true, confirmButtonText: 'Confirmar Devolução', cancelButtonText: 'Cancelar', confirmButtonColor: '#dc3545', 
        }).then((result) => { if (result.isConfirmed) this.executarDevolucao(this.currentItemInModal.id); });
    }

    async executarDevolucao(itemId) {
        try {
            const response = await fetch(`${this.API_BASE_URL}/devolucao/${itemId}`, { method: 'POST', headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${this.user}` } });
            if (!response.ok) throw new Error(await response.text() || 'Falha na devolução.');
            await Swal.fire({ title: 'Item Devolvido!', text: `O item "${this.currentItemInModal.nome}" foi removido da sua coleção.`, icon: 'success' });
            await this.buscaDadosusuario(); 
            this.atualizarStatusItemLocal(itemId, false); 
            this.preencherModalItem(this.currentItemInModal);
            this.renderizaItens(); this.renderizarTodosOsItens(); this.renderizarMeusItens(); this.preencheCarrousel();
        } catch (error) { console.error('Erro ao devolver item:', error); Swal.fire({ title: 'Erro na Devolução', text: error.message, icon: 'error' }); }
    }

    atualizarStatusItemLocal(itemId, isAdquiridoStatus) {
        if (isAdquiridoStatus) this.itensAdquiridosSet.add(itemId); else this.itensAdquiridosSet.delete(itemId);
        const updateItem = (list) => { const i = list.find(x => x.id === itemId); if(i) i.isAdquirido = isAdquiridoStatus; return i; };
        const itemNaLoja = updateItem(this.itens);
        const itemEmTodos = updateItem(this.todosOsItens);
        if (this.currentItemInModal && this.currentItemInModal.id === itemId) this.currentItemInModal = itemNaLoja || itemEmTodos;
    }

    formatarData(dataString) {
        if (!dataString) return 'N/A';
        try { return new Date(dataString).toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric' }); } catch (e) { return dataString; }
    }

    sanitizarTexto(texto) { const div = document.createElement('div'); div.textContent = texto; return div.innerHTML; }
    sanitizarUrl(url) { try { new URL(url); return url; } catch { return ''; } }
    mostrarErro(mensagem, gridElement = this.cosmeticosGrid) {
        if (gridElement) gridElement.innerHTML = `<div class="col-12 w-100 d-flex flex-column justify-content-center align-items-center" style="grid-column: 1 / -1; min-height: 150px;"><i class="bi bi-exclamation-triangle text-danger mb-3" style="font-size: 2rem;"></i><p class="text-center text-danger fs-5 m-0">${this.sanitizarTexto(mensagem)}</p></div>`;
    }
    
    buscarItemGlobalmente(id) {
        let item = this.itens.find(i => i.id === id);
        if (item) return item;
        if (this.userData && this.userData.itensAdquiridos) {
            item = this.userData.itensAdquiridos.find(i => i.id === id);
            if (item) return item;
        }
        if (this.todosOsItens && this.todosOsItens.length > 0) {
            item = this.todosOsItens.find(i => i.id === id);
            if (item) return item;
        }
        return null;
    }
}

class ValidadorItem {
    constructor(id, nome, tipo, raridade, preco, urlImagem, descricao, isNew, isForSale, isAdquirido, dataInclusao, cores, isBundle, bundleItems) {
        this.id = id;
        this.nome = nome;
        this.tipo = tipo;
        this.raridade = raridade;
        this.preco = preco;
        this.urlImagem = urlImagem;
        this.descricao = descricao;
        this.isNew = isNew;
        this.isForSale = isForSale;
        this.isAdquirido = isAdquirido;
        this.dataInclusao = dataInclusao;
        this.cores = cores;
        this.isBundle = isBundle;
        this.bundleItems = bundleItems; 
    }

    validaDados() {
        return {
            id: this.id,
            nome: (typeof this.nome === 'string' && this.nome.trim() !== '') ? this.nome.trim() : 'Sem nome',
            tipo: (typeof this.tipo === 'string' && this.tipo.trim() !== '') ? this.tipo.trim() : 'Cosmético',
            raridade: (typeof this.raridade === 'string' && this.raridade.trim() !== '') ? this.raridade.trim() : 'Comum',
            preco: (typeof this.preco === 'number' && this.preco >= 0) ? this.preco : null,
            urlImagem: (typeof this.urlImagem === 'string') ? this.urlImagem : null,
            descricao: (typeof this.descricao === 'string') ? this.descricao.trim() : '',
            isNew: typeof this.isNew === 'boolean' ? this.isNew : false,
            isForSale: typeof this.isForSale === 'boolean' ? this.isForSale : false,
            isAdquirido: typeof this.isAdquirido === 'boolean' ? this.isAdquirido : false,
            dataInclusao: this.dataInclusao,
            cores: Array.isArray(this.cores) ? this.cores : [],
            isBundle: typeof this.isBundle === 'boolean' ? this.isBundle : false,
            bundleItems: Array.isArray(this.bundleItems) ? this.bundleItems : [] 
        };
    }
}