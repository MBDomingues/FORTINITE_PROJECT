/**
 * Classe principal VitrineJS
 * Responsável por gerenciar toda a lógica do front-end da loja:
 * usuários, listagem de itens, compras, histórico e manipulação do DOM.
 */
class VitrineJS {
    constructor(userToken) {
        // Inicialização de estados e configurações
        this.user = userToken || null; // Token JWT do usuário
        this.API_BASE_URL = 'http://130.213.12.104:8080/api/v1'; // Endpoint base da API
        
        // Arrays e flags para controle de dados da Loja
        this.itens = []; 
        this.userData = null;

        // Controle da aba "Todos os Itens" (Paginação/Carregamento)
        this.todosOsItens = []; 
        this.todosOsItensCarregados = false;
        
        // Controle da aba "Usuários" (Admin)
        this.usuarios = [];
        this.usuariosCarregados = false;
        
        // Controle do Histórico de Transações
        this.historico = [];
        this.historicoCarregado = false;
        
        // Set para verificação rápida de itens já comprados (O(1))
        this.itensAdquiridosSet = new Set();
        
        // Referências para modais
        this.itemModalElement = null;
        this.currentItemInModal = null;

        // Inicia a aplicação
        this.init();
    }

    // Método inicializador: Orquestra o carregamento da página
    async init() {
        this.pegaElementos(); // 1. Mapeia elementos HTML
        await this.verificaUsuario(); // 2. Valida sessão do usuário
        this.buscaItensDisponiveis(); // 3. Carrega vitrine inicial
    }

    // Mapeia todos os elementos do DOM e adiciona Event Listeners
    pegaElementos() {
        // Elementos principais da UI
        this.cosmeticosGrid = document.getElementById('shop-grid'); 
        this.carrouselItems = document.getElementById('carouselItems');
        this.navItens = document.getElementById('nav-itens');

        if (!this.cosmeticosGrid || !this.carrouselItems || !this.navItens) {
            console.error('Elementos essenciais da "Loja" não encontrados');
            return;
        }

        // Modais e Botões de Ação
        this.perfilModal = document.getElementById('userProfileModal');
        this.itemModalElement = document.getElementById('itemModal');
        
        this.btnBuy = document.getElementById('btn-buy');
        if (this.btnBuy) {
            this.btnBuy.addEventListener('click', () => this.handleCompraClick());
        }
        
        this.btnDevolver = document.getElementById('btn-devolver');
        if (this.btnDevolver) {
            this.btnDevolver.addEventListener('click', () => this.handleDevolucaoClick());
        }
        
        // --- Filtros da Aba "Loja" ---
        this.shopTypeFilter = document.getElementById('shop-typeFilter');
        this.shopRarityFilter = document.getElementById('shop-rarityFilter');
        this.shopSearchInput = document.getElementById('shop-searchInput');

        // Adiciona eventos de mudança/input para filtragem em tempo real
        if (this.shopTypeFilter) this.shopTypeFilter.addEventListener('change', () => this.renderizaItens());
        if (this.shopRarityFilter) this.shopRarityFilter.addEventListener('change', () => this.renderizaItens());
        if (this.shopSearchInput) this.shopSearchInput.addEventListener('input', () => this.renderizaItens());
        
        // --- Aba "Todos os Itens" ---
        this.allItemsTab = document.getElementById('all-items-tab');
        this.allItemsGrid = document.getElementById('all-items-grid');
        this.allTypeFilter = document.getElementById('all-typeFilter');
        this.allRarityFilter = document.getElementById('all-rarityFilter');
        this.allSearchInput = document.getElementById('all-searchInput');

        // Carrega dados apenas quando a aba recebe foco
        if (this.allItemsTab && this.allItemsGrid) {
            this.allItemsTab.addEventListener('show.bs.tab', () => {
                this.handleAllItemsTabFocus();
            });
        }

        // --- Aba "Usuários" ---
        this.usersTabContainer = document.getElementById('users-tab-container');
        this.usersTab = document.getElementById('users-tab');
        this.usersListContainer = document.getElementById('users-list');
        this.userSearchInput = document.getElementById('userSearchInput');
        this.userSortFilter = document.getElementById('userSortFilter');

        if (this.usersTab && this.usersListContainer) {
            this.usersTab.addEventListener('show.bs.tab', () => {
                this.handleUsersTabFocus();
            });
        }
        
        // --- Aba "Meus Itens" ---
        this.myItemsTabContainer = document.getElementById('my-items-tab-container');
        this.myItemsTab = document.getElementById('my-items-tab');
        this.myItemsGrid = document.getElementById('my-items-grid');
        
        if (this.myItemsTab && this.myItemsGrid) {
            this.myItemsTab.addEventListener('show.bs.tab', () => {
                this.renderizarMeusItens(); 
            });
        }

        // --- Abas internas do Modal de Perfil ---
        this.historyTabButton = document.getElementById('profile-tab-history-tab');
        this.historyListContainer = document.getElementById('modal-user-history');
        this.itemsTabButton = document.getElementById('profile-tab-items-tab'); 

        console.log('VitrineJS inicializado com sucesso');
    }

    // Verifica se há usuário logado e ajusta a visibilidade das abas restritas
    async verificaUsuario() {
        if (this.user) {
            await this.buscaDadosusuario();
            // Exibe abas restritas para usuários logados
            if (this.myItemsTabContainer) this.myItemsTabContainer.style.display = 'block';
            if (this.usersTabContainer) this.usersTabContainer.style.display = 'block'; 
        } else {
            // Renderiza botões de Login/Cadastro se não houver sessão
            this.navItens.innerHTML = `<a href="/login.html" class="btn btn-login">Entrar</a>
                                       <a href="/cadastro.html" class="btn btn-signup">Criar Conta</a>`;
            
            if (this.myItemsTabContainer) this.myItemsTabContainer.style.display = 'none';
            if (this.usersTabContainer) this.usersTabContainer.style.display = 'none';
        }
    }

    // Busca dados do perfil do usuário logado (/perfis/me)
    buscaDadosusuario() {
        return fetch(`${this.API_BASE_URL}/perfis/me`, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${this.user}`
            }
        })
        .then(response => {
            if (!response.ok) {
                // Se token inválido, faz logout forçado
                localStorageManager.removeToken(); 
                throw new Error('Erro ao buscar dados do usuário');
            }
            return response.json();
        })
        .then(data => {
            console.log('Dados do usuário:', data);
            this.userData = data;
            
            // Atualiza Set de itens para consulta rápida (O(1))
            if (data.itensAdquiridos && Array.isArray(data.itensAdquiridos)) {
                this.itensAdquiridosSet = new Set(data.itensAdquiridos.map(item => item.id));
            }
            
            this.atualizaNavUsuario(data);
            this.buscaHistoricoUsuario();
        })
        .catch(error => {
            console.error('Erro na requisição:', error);
            this.user = null;
            localStorageManager.removeToken();
            // Reseta navbar para estado deslogado
            this.navItens.innerHTML = `<a href="/login.html" class="btn btn-login">Entrar</a>
                                       <a href="/cadastro.html" class="btn btn-signup">Criar Conta</a>`;
        });
    }

    // Atualiza a barra de navegação com saldo de V-Bucks e Menu de Perfil
    atualizaNavUsuario(userData) {
        const creditosFormatados = (userData.creditos || 0).toLocaleString('pt-BR');
        
        this.navItens.innerHTML = `
            <span class="nav-creditos d-flex align-items-center me-3">
                ${creditosFormatados} V-Bucks
            </span>
            <button type="button" class="btn btn-perfil" data-bs-toggle="modal" 
                    data-bs-target="#userProfileModal" id="nav-perfil-btn"> 
                <i class="bi bi-person-fill me-2"></i> Perfil
            </button>
            <button id="nav-logout" class="btn btn-logout">
                <i class="bi bi-box-arrow-right me-2"></i> Sair
            </button>
        `;
        
        // Configura evento de Logout
        const logoutButton = document.getElementById('nav-logout');
        if (logoutButton) {
            logoutButton.addEventListener('click', () => {
                localStorageManager.removeToken(); 
                this.user = null;
                window.location.href = 'index.html';
            });
        }
        
        // Configura abertura do modal de perfil
        const navPerfilButton = document.getElementById('nav-perfil-btn');
        if (navPerfilButton) {
            navPerfilButton.addEventListener('click', () => {
                this.preencherModalPerfil(this.userData);
            });
        }
    }

    // Preenche o modal de perfil com dados do usuário (Info + Inventário)
    preencherModalPerfil(userData) {
        if (!userData) {
            console.warn('Dados do usuário nulos, não é possível preencher o modal.');
            return;
        }

        console.log('Preenchendo modal com:', userData);

        const email = userData.email || 'Usuário';
        const nomeUsuario = userData.nome || email.split('@')[0];
        const creditosFormatados = (userData.creditos || 0).toLocaleString('pt-BR');

        // Preenche estatísticas básicas (V-Bucks, Qtd Itens)
        document.getElementById('modal-avatar').innerHTML = `<i class="bi bi-file-person"></i>`;
        document.getElementById('modal-user-name').textContent = nomeUsuario;
        document.getElementById('stat-items').textContent = userData.itensAdquiridos?.length || 0;
        document.getElementById('stat-vbucks').textContent = creditosFormatados;
        document.getElementById('modal-user-email').textContent = email;
        
        // Renderiza lista de itens adquiridos (Mini Cards)
        const itensAdquiridos = userData.itensAdquiridos || [];
        const itensContainer = document.getElementById('modal-user-items');
        
        if (itensAdquiridos.length > 0) {
            itensContainer.innerHTML = ''; 
            
            itensAdquiridos.forEach(item => {
                const nome = this.sanitizarTexto(item.nome || 'Item');
                const raridade = item.raridade || 'Comum';
                const classeRaridade = this.obterClasseRaridade(raridade);
                
                const imagem = item.urlImagem 
                    ? `<img src="${this.sanitizarUrl(item.urlImagem)}" alt="${nome}">`
                    : '<i class="bi bi-question-lg"></i>';

                const itemCardHTML = `
                    <div class="user-item-mini-card">
                        <div class="user-item-mini-image rarity-bg-${classeRaridade}">
                            ${imagem}
                        </div>
                        <div class="user-item-mini-name">
                            ${nome}
                        </div>
                    </div>
                `;
                itensContainer.innerHTML += itemCardHTML;
            });
        } else {
            itensContainer.innerHTML = '<p class="text-center text-light">Nenhum item adquirido.</p>';
        }

        // Lógica de exibição da aba Histórico: 
        // Só mostra se o usuário estiver vendo o PRÓPRIO perfil.
        const historyTabContainer = document.getElementById('modal-history-section'); 
        
        if (this.user && this.userData && userData.id === this.userData.id) {
            if (historyTabContainer) historyTabContainer.style.display = 'block';
            this.renderizarHistorico(); 
        } else {
            if (historyTabContainer) historyTabContainer.style.display = 'none';
            
            // Reseta para aba de itens se histórico estiver oculto
            const itemsTabButton = document.getElementById('profile-tab-items-tab');
            if (itemsTabButton) {
                bootstrap.Tab.getOrCreateInstance(itemsTabButton).show();
            }
        }
    }

    // Busca itens da Loja na API (/cosmeticos/loja)
    async buscaItensDisponiveis() {
        const headers = { 'Content-Type': 'application/json' };
        if (this.user) headers['Authorization'] = `Bearer ${this.user}`;
        
        try {
            const response = await fetch(`${this.API_BASE_URL}/cosmeticos/loja`, { headers });
            if (!response.ok) throw new Error(`Erro HTTP: ${response.status}`);

            const itensDaApi = await response.json();
            if (Array.isArray(itensDaApi)) {
                
                // Mapeia e valida os dados recebidos
                this.itens = itensDaApi.map(item => {
                    const isAdquirido = this.itensAdquiridosSet.has(item.id);
                    
                    const validador = new ValidadorItem(
                        item.id, item.nome, item.tipo, item.raridade, 
                        item.preco, item.urlImagem, item.descricao,
                        item.isNew, item.isForSale, 
                        isAdquirido, 
                        item.dataInclusao
                    );
                    return validador.validaDados();
                });
                
                console.log(`Total de ${this.itens.length} itens da LOJA carregados.`);
                this.renderizaItens(); // Exibe no Grid
                this.preencheCarrousel(); // Exibe no Carrousel
            
            } else {
                throw new Error("API /loja/todos não retornou um array.");
            }
        } catch (error) {
            console.error('Erro ao buscar itens da loja:', error);
            this.mostrarErro('Erro ao carregar itens da loja. Tente novamente.', this.cosmeticosGrid);
        }
    }

    // Renderiza grid da aba "Loja" com filtros (Frontend Filtering)
    renderizaItens() {
        if (!this.cosmeticosGrid) return;

        // Captura valores dos filtros do DOM
        const tipo = this.shopTypeFilter.value;
        const raridadeValue = this.shopRarityFilter.value;
        const busca = this.shopSearchInput.value.toLowerCase();

        // Filtra array localmente
        let itensFiltrados = this.itens.filter(item => {
            const classeRaridade = this.obterClasseRaridade(item.raridade); 
            const matchTipo = !tipo || item.tipo.toLowerCase() === tipo.toLowerCase();
            const matchRaridade = !raridadeValue || classeRaridade === raridadeValue;
            const matchBusca = !busca || item.nome.toLowerCase().includes(busca);
            return matchTipo && matchRaridade && matchBusca;
        });
        
        this.cosmeticosGrid.innerHTML = '';

        if (itensFiltrados.length === 0) {
            this.cosmeticosGrid.innerHTML = '<div class="col-12"><p class="text-center text-light fs-5">Nenhum item encontrado.</p></div>';
            return;
        }

        // Ordenação customizada por peso de raridade
        const getRaridadePeso = (raridade) => {
            const r = raridade || 'Comum'; 
            if (r.startsWith('Série')) return 7;
            switch (r) {
                case 'Lendário': return 6;
                case 'Épico': return 5;
                case 'Raro': return 4;
                case 'Incomum': return 3;
                case 'Comum': return 2;
                default: return 1;
            }
        };

        itensFiltrados.sort((a, b) => { 
            const pesoA = getRaridadePeso(a.raridade);
            const pesoB = getRaridadePeso(b.raridade);
            if (pesoA !== pesoB) return pesoB - pesoA; // Mais raro primeiro
            const tipoA = a.tipo || '';
            const tipoB = b.tipo || '';
            return tipoA.localeCompare(tipoB);
        });

        // Cria fragmento de documento para performance na inserção
        const fragmento = document.createDocumentFragment();
        for (const item of itensFiltrados) {
            const card = this.criarCard(item);
            fragmento.appendChild(card);
        }
        this.cosmeticosGrid.appendChild(fragmento);
    }

    // --- Lógica da Aba "Todos os Itens" ---

    // Gerencia o foco na aba e debounce da busca
    async handleAllItemsTabFocus() {
        if (!this.todosOsItensCarregados) {
            console.log('Carregando itens iniciais (Top 40 mais novos)...');
            await this.buscaTodosOsItens();
            this.todosOsItensCarregados = true;
        }

        // Listeners para filtros desta aba específica
        if (this.allTypeFilter) this.allTypeFilter.onchange = () => this.buscaTodosOsItens();
        if (this.allRarityFilter) this.allRarityFilter.onchange = () => this.buscaTodosOsItens();
        
        // Debounce: Aguarda 500ms após digitação para buscar
        if (this.allSearchInput) {
            let timeoutId;
            this.allSearchInput.oninput = () => {
                clearTimeout(timeoutId);
                timeoutId = setTimeout(() => {
                    this.buscaTodosOsItens();
                }, 500);
            };
        }
    }

    // Busca itens na API com paginação e filtros via Query Params (Backend Filtering)
    async buscaTodosOsItens() {
        if (!this.allItemsGrid) return;
        
        const headers = { 'Content-Type': 'application/json' };
        if (this.user) headers['Authorization'] = `Bearer ${this.user}`;

        // Captura filtros
        const tipo = this.allTypeFilter ? this.allTypeFilter.value : '';
        const raridadeRaw = this.allRarityFilter ? this.allRarityFilter.value : '';
        const busca = this.allSearchInput ? this.allSearchInput.value.trim() : '';

        // Tradução HTML Value -> API Value
        const mapaRaridade = {
            'serie': 'Série',
            'legendary': 'Lendário',
            'epic': 'Épico',
            'rare': 'Raro',
            'uncommon': 'Incomum',
            'common': 'Comum'
        };
        const raridadeAPI = mapaRaridade[raridadeRaw] || '';

        // Construção da URL
        const url = new URL(`${this.API_BASE_URL}/cosmeticos`);
        if (busca) url.searchParams.append('nome', busca);
        if (tipo) url.searchParams.append('tipo', tipo);
        if (raridadeAPI) url.searchParams.append('raridade', raridadeAPI);

        // Paginação Fixa (Top 40)
        url.searchParams.append('page', '0');          
        url.searchParams.append('size', '40');        
        url.searchParams.append('sort', 'dataInclusao,desc'); 

        try {
            this.allItemsGrid.innerHTML = '<div class="col-12"><p class="text-center text-light fs-5"><span class="spinner-border spinner-border-sm"></span> Buscando...</p></div>';

            const response = await fetch(url.toString(), { headers }); 
            if (!response.ok) throw new Error(`Erro HTTP: ${response.status}`);
            
            const dadosDaPagina = await response.json();
            const listaItens = dadosDaPagina.content || (Array.isArray(dadosDaPagina) ? dadosDaPagina : []);

            this.todosOsItens = listaItens.map(item => {
                const isAdquirido = this.itensAdquiridosSet.has(item.id);
                return new ValidadorItem(
                    item.id, item.nome, item.tipo, item.raridade,
                    item.preco, item.urlImagem, item.descricao,
                    item.isNew, item.isForSale, isAdquirido, item.dataInclusao
                ).validaDados();
            });

            this.renderizarTodosOsItens(); 

        } catch (error) {
            console.error('Erro ao buscar itens:', error);
            this.mostrarErro('Erro ao carregar itens. Tente novamente.', this.allItemsGrid);
        }
    }
    
    // Renderiza itens da busca global
    renderizarTodosOsItens() {
        if (!this.allItemsGrid) return;

        this.allItemsGrid.innerHTML = '';
        
        if (this.todosOsItens.length === 0) {
            this.allItemsGrid.innerHTML = '<div class="col-12"><p class="text-center text-light fs-5">Nenhum item encontrado.</p></div>';
            return;
        }

        // Ordenação visual local dos itens recebidos
        const getRaridadePeso = (raridade) => {
            const r = raridade || 'Comum'; 
            if (r.toLowerCase().includes('série') || r.toLowerCase().includes('serie')) return 7;
            switch (r) {
                case 'Lendário': return 6;
                case 'Épico': return 5;
                case 'Raro': return 4;
                case 'Incomum': return 3;
                case 'Comum': return 2;
                default: return 1;
            }
        };

        const itensOrdenados = [...this.todosOsItens].sort((a, b) => {
            const pesoA = getRaridadePeso(a.raridade);
            const pesoB = getRaridadePeso(b.raridade);
            if (pesoA !== pesoB) return pesoB - pesoA;
            return (a.nome || '').localeCompare(b.nome || '');
        });
        
        const fragmento = document.createDocumentFragment();
        for (const item of itensOrdenados) {
            const card = this.criarCard(item); 
            fragmento.appendChild(card);
        }
        
        this.allItemsGrid.appendChild(fragmento);
    }

    // --- Aba "Meus Itens" ---

    // Renderiza itens adquiridos pelo usuário logado
    renderizarMeusItens() {
        if (!this.myItemsGrid) return;
        
        this.myItemsGrid.innerHTML = ''; 

        const itensAdquiridos = this.userData ? (this.userData.itensAdquiridos || []) : [];

        if (itensAdquiridos.length === 0) {
            this.myItemsGrid.innerHTML = '<div class="col-12"><p class="text-center text-light fs-5">Você ainda não adquiriu nenhum item.</p></div>';
            return;
        }

        // Ordenação
        const getRaridadePeso = (raridade) => {
            const r = raridade || 'Comum'; 
            if (r.startsWith('Série')) return 7;
            switch (r) {
                case 'Lendário': return 6;
                case 'Épico': return 5;
                case 'Raro': return 4;
                case 'Incomum': return 3;
                case 'Comum': return 2;
                default: return 1;
            }
        };
        itensAdquiridos.sort((a, b) => {
            const pesoA = getRaridadePeso(a.raridade);
            const pesoB = getRaridadePeso(b.raridade);
            if (pesoA !== pesoB) return pesoB - pesoA;
            const tipoA = a.tipo || '';
            const tipoB = b.tipo || '';
            return tipoA.localeCompare(tipoB);
        });
        
        const fragmento = document.createDocumentFragment();
        for (const itemData of itensAdquiridos) {
            const validador = new ValidadorItem(
                itemData.id, itemData.nome, itemData.tipo, itemData.raridade,
                itemData.preco, itemData.urlImagem, itemData.descricao,
                itemData.isNew, itemData.isForSale, 
                true, // Marca forçadamente como adquirido
                itemData.dataInclusao
            );
            const item = validador.validaDados();
            const card = this.criarCard(item); 
            fragmento.appendChild(card);
        }
        
        this.myItemsGrid.appendChild(fragmento);
    }

    // --- Aba "Usuários" ---

    // Carrega lista de usuários (Primeira vez apenas)
    async handleUsersTabFocus() {
        if (this.usuariosCarregados) return; 
        
        if (!this.user) {
            this.mostrarErro('Você precisa estar logado para ver os usuários.', this.usersListContainer);
            return;
        }
        console.log('Carregando "Usuários" pela primeira vez...');
        await this.buscaUsuarios();

        // Listeners de filtro/busca
        if (this.userSearchInput) {
            this.userSearchInput.addEventListener('input', () => this.renderizarUsuarios());
        }
        if (this.userSortFilter) {
            this.userSortFilter.addEventListener('change', () => this.renderizarUsuarios());
        }
    }

    // Busca todos os usuários na API (/perfis)
    async buscaUsuarios() {
        if (!this.usersListContainer) return;
        
        try {
            this.usersListContainer.innerHTML = '<div class="col-12"><p class="text-center text-light fs-5">Carregando usuários...</p></div>';

            const response = await fetch(`${this.API_BASE_URL}/perfis`, {
                headers: { 'Authorization': `Bearer ${this.user}` }
            }); 
            
            if (!response.ok) {
                if (response.status === 403) throw new Error('Você não tem permissão para ver esta lista.');
                throw new Error(`Erro HTTP: ${response.status}`);
            }
            
            const dadosDaPagina = await response.json();
            
            if (dadosDaPagina.content && Array.isArray(dadosDaPagina.content)) {
                this.usuarios = dadosDaPagina.content;
                this.usuariosCarregados = true;
                this.renderizarUsuarios();
            } else {
                throw new Error("A API /perfis não retornou um objeto com a propriedade 'content'.");
            }
        } catch (error) {
            console.error('Erro ao buscar usuários:', error);
            this.mostrarErro(error.message, this.usersListContainer);
        }
    }

    // Renderiza a tabela de usuários
    renderizarUsuarios() {
        if (!this.usersListContainer) return;

        const busca = this.userSearchInput.value.toLowerCase();
        const sortBy = this.userSortFilter.value;

        // Filtragem local por email
        let usuariosFiltrados = this.usuarios.filter(user => {
            const matchBusca = !busca || user.email.toLowerCase().includes(busca);
            return matchBusca;
        });

        // Ordenação local
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
        
        if (usuariosFiltrados.length === 0) {
            this.usersListContainer.innerHTML = '<p class="text-center text-light fs-5">Nenhum usuário encontrado.</p>';
            return;
        }

        // Monta Tabela Responsiva HTML
        const tableHTML = `
            <div class="table-responsive">
                <table class="table table-dark table-striped table-hover align-middle text-nowrap custom-mobile-table">
                    <thead>
                        <tr>
                            <th scope="col">ID</th>
                            <th scope="col">Email</th>
                            <th scope="col">V-Bucks</th>
                            <th scope="col" class="text-end">Ação</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${usuariosFiltrados.map(user => `
                            <tr>
                                <td data-label="ID" scope="row" class="fw-bold">#${user.id}</td>
                                <td data-label="Email">${this.sanitizarTexto(user.email)}</td>
                                <td data-label="V-Bucks" class="fw-bold">${user.creditos.toLocaleString('pt-BR')}</td>
                                <td data-label="Ação" class="text-end">
                                    <button class="btn btn-sm btn-info btn-visualizar-usuario" data-userid="${user.id}">
                                        <i class="bi bi-eye-fill"></i> <span class="d-none d-sm-inline">Visualizar</span>
                                    </button>
                                </td>
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
            </div>
        `;
        
        this.usersListContainer.innerHTML = tableHTML;

        // Adiciona eventos aos botões "Visualizar"
        this.usersListContainer.querySelectorAll('.btn-visualizar-usuario').forEach(button => {
            button.addEventListener('click', (e) => this.handleVisualizarUsuarioClick(e));
        });
    }

    // Ação ao clicar para ver outro usuário
    handleVisualizarUsuarioClick(event) {
        const userId = event.currentTarget.dataset.userid;
        if (!userId) return;
        this.buscaDadosUsuarioPorId(userId);
    }

    // Busca detalhes de um usuário específico e abre modal
    async buscaDadosUsuarioPorId(id) {
        Swal.fire({
            title: 'Buscando usuário...',
            allowOutsideClick: false,
            didOpen: () => Swal.showLoading()
        });

        try {
            const response = await fetch(`${this.API_BASE_URL}/perfis/${id}`, {
                headers: { 'Authorization': `Bearer ${this.user}` }
            });

            if (!response.ok) throw new Error('Não foi possível buscar os dados do usuário.');
            
            const dadosUsuario = await response.json();
            
            Swal.close();
            this.preencherModalPerfil(dadosUsuario);
            
            const modal = bootstrap.Modal.getOrCreateInstance(this.perfilModal);
            modal.show();

        } catch (error) {
            console.error('Erro ao buscar usuário por ID:', error);
            Swal.fire({ title: 'Erro', text: error.message, icon: 'error' });
        }
    }


    // --- Histórico de Transações ---

    // Busca histórico de compras/devoluções
    async buscaHistoricoUsuario() {
        if (this.historicoCarregado || !this.user) return;
        
        try {
            const response = await fetch(`${this.API_BASE_URL}/perfis/me/historico`, {
                headers: { 'Authorization': `Bearer ${this.user}` }
            });
            if (!response.ok) throw new Error('Não foi possível carregar o histórico.');
            
            const dadosPagina = await response.json();
            if (dadosPagina.content && Array.isArray(dadosPagina.content)) {
                this.historico = dadosPagina.content;
                this.historicoCarregado = true;
            }
        } catch (error) {
            console.error("Erro ao buscar histórico:", error);
            if (this.historyListContainer) {
                this.historyListContainer.innerHTML = `<p class="text-center text-danger">Não foi possível carregar o histórico.</p>`;
            }
        }
    }

    // Renderiza a lista de histórico
    renderizarHistorico() {
        if (!this.historyListContainer) return;
        
        this.historyListContainer.innerHTML = ''; // Limpa

        if (this.historico.length === 0) {
            this.historyListContainer.innerHTML = '<p class="text-center text-light">Nenhuma transação encontrada.</p>';
            return;
        }

        this.historico.forEach(item => {
            const isCompra = item.tipo === 'COMPRA';
            const tipoClasse = isCompra ? 'tipo-compra' : 'tipo-devolucao';
            const iconClasse = isCompra ? 'bi-cart-dash-fill' : 'bi-arrow-counterclockwise';
            const valorPrefixo = isCompra ? '-' : '+';
            const dataFormatada = this.formatarData(item.dataTransacao);

            const itemHTML = `
                <div class="history-item ${tipoClasse}">
                    <div class="history-item-icon">
                        <i class="bi ${iconClasse}"></i>
                    </div>
                    <div class="history-item-details">
                        <div class="history-item-title">${this.sanitizarTexto(item.cosmeticoNome)}</div>
                        <div class="history-item-date">${dataFormatada}</div>
                    </div>
                    <div class="history-item-value">
                        ${valorPrefixo}${item.valor.toLocaleString('pt-BR')}
                    </div>
                </div>
            `;
            this.historyListContainer.innerHTML += itemHTML;
        });
    }


    // --- Métodos Auxiliares (UI, Cards, Carrousel) ---

    // Preenche o carrousel com os Top 5 itens mais caros
    preencheCarrousel() {
        if (!this.carrouselItems) return;
        if (this.itens.length === 0) {
            this.carrouselItems.innerHTML = '<div class="carousel-item active"><p class="text-center text-light">Nenhum item disponível</p></div>';
            return;
        }
        const top5MaisCaros = [...this.itens]
            .sort((a, b) => (b.preco || 0) - (a.preco || 0))
            .slice(0, 5);
        
        const fragmento = document.createDocumentFragment();
        top5MaisCaros.forEach((item, index) => {
            const div = this.criarItemCarrousel(item, index === 0);
            fragmento.appendChild(div);
        });
        this.carrouselItems.innerHTML = '';
        this.carrouselItems.appendChild(fragmento);
    }
    
    // Cria elemento HTML para um slide do carrousel
    criarItemCarrousel(item, isActive) {
        const div = document.createElement('div');
        div.className = `carousel-item ${isActive ? 'active' : ''}`;
        
        const imagem = item.urlImagem 
            ? `<img src="${this.sanitizarUrl(item.urlImagem)}" alt="${this.sanitizarTexto(item.nome || 'Item')}" />` 
            : '<div class="placeholder-image">Sem imagem</div>';
            
        const classeRaridade = this.obterClasseRaridade(item.raridade); 

        div.innerHTML = `
            <div class="row g-0 h-100">
                <div class="col-md-4 h-100 carousel-image-container bg-rarity-${classeRaridade}">
                    ${imagem}
                </div>
                <div class="col-md-8 d-flex flex-column justify-content-center p-4 p-md-5">
                    <div class="carousel-caption position-relative text-start">
                        <h3 class="fw-bold">${this.sanitizarTexto(item.nome || 'Sem nome')}</h3>
                        <p class="lead">${this.sanitizarTexto(item.descricao || 'Sem descrição')}</p>
                        <div class="price mt-3">
                            ${item.preco ? `${item.preco} V-Bucks` : 'Item indisponível'}
                        </div>
                    </div>
                </div>
            </div>
        `;
        return div;
    }

    // Cria o HTML do Card de um Item
    criarCard(item) {
        const card = document.createElement('div');
        card.className = 'col';

        const imagem = item.urlImagem 
            ? `<img src="${this.sanitizarUrl(item.urlImagem)}" alt="${this.sanitizarTexto(item.nome || 'Item')}" />` 
            : '<div class="placeholder-image">Sem imagem</div>';

        const classeRaridade = this.obterClasseRaridade(item.raridade);

        // Badges condicionais
        const newBadge = item.isNew ? `<span class="badge status-badge badge-new">Novo</span>` : '';
        const forSaleBadge = (item.isForSale && !item.isAdquirido) ? `<span class="badge status-badge badge-for-sale">À Venda</span>` : '';
        const adquiridoBadge = (this.user && item.isAdquirido) ? `<span class="badge status-badge badge-adquirido">Adquirido</span>` : '';

        card.innerHTML = `
            <div class="product-card">
                <div class="product-image bg-rarity-${classeRaridade}">
                    ${imagem}
                </div>
                <div class="card-body">
                    <div class="product-status-badges mb-2">
                        ${newBadge} ${forSaleBadge} ${adquiridoBadge}
                    </div>
                    <h5 class="product-name">${this.sanitizarTexto(item.nome || 'Sem nome')}</h5>
                    <p class="product-type">${this.sanitizarTexto(item.tipo || 'Cosmético')}</p>
                    
                    <div class="product-price mt-2">
                        ${item.preco ? `${item.preco} V-Bucks` : 'Item indisponível'}
                    </div>
                </div>
            </div>
        `;

        const cardClicavel = card.querySelector('.product-card');
        if (cardClicavel) {
            cardClicavel.addEventListener('click', () => {
                this.abrirModalItem(item);
            });
        }

        return card;
    }

    // Abre modal de detalhes do item
    abrirModalItem(item) {
        if (!this.itemModalElement) {
            console.error('Elemento do modal de item não foi encontrado!');
            return;
        }
        
        this.preencherModalItem(item);
        
        const modal = bootstrap.Modal.getOrCreateInstance(this.itemModalElement);
        modal.show();
    }

    // Preenche o modal de detalhes (lógica visual de compra/devolução)
    preencherModalItem(item) {
        this.currentItemInModal = item;
        const modal = this.itemModalElement;
        if (!modal) return;

        // 1. Imagem e Fundo
        const imgContainer = modal.querySelector('#modal-item-image');
        if (imgContainer) {
            // Remove classes de raridade antigas e adiciona a nova
            imgContainer.className = imgContainer.className
                .split(' ')
                .filter(c => !c.startsWith('bg-rarity-'))
                .join(' ');

            const classeRaridade = this.obterClasseRaridade(item.raridade);
            imgContainer.classList.add(`bg-rarity-${classeRaridade}`);

            if (item.urlImagem) {
                imgContainer.innerHTML = `<img src="${this.sanitizarUrl(item.urlImagem)}" alt="${this.sanitizarTexto(item.nome)}"/>`;
            } else {
                imgContainer.innerHTML = '<div class="placeholder-image">🎮</div>';
            }
        }

        // 2. Badges
        const badgeNew = modal.querySelector('#modal-item-badge-new');
        if (badgeNew) badgeNew.style.display = item.isNew ? 'inline-block' : 'none';
        
        const badgeSale = modal.querySelector('#modal-item-badge-sale');
        if (badgeSale) badgeSale.style.display = (item.isForSale && !item.isAdquirido) ? 'inline-block' : 'none';

        const rarityBadge = modal.querySelector('#modal-item-rarity');
        if (rarityBadge) {
            const classeRaridade = this.obterClasseRaridade(item.raridade);
            rarityBadge.textContent = this.sanitizarTexto(item.raridade);
            rarityBadge.className = `badge rarity-${classeRaridade}`;
        }

        // 3. Disponibilidade e Botões
        const availability = modal.querySelector('#modal-item-availability');
        const btnBuy = modal.querySelector('#btn-buy'); 
        const btnDevolver = modal.querySelector('#btn-devolver');

        if (availability) {
            const title = modal.querySelector('#modal-availability-title');
            const text = modal.querySelector('#modal-availability-text');
            const icon = availability.querySelector('i');

            if (this.user && item.isAdquirido) {
                // Item já adquirido
                title.textContent = 'Adquirido';
                text.textContent = 'Este item já está na sua coleção.';
                icon.className = 'bi bi-check-all';
                availability.className = 'item-availability status-adquirido';
                if (btnBuy) btnBuy.style.display = 'none'; 
                if (btnDevolver) btnDevolver.style.display = 'flex'; 
            } else if (item.isForSale) {
                // Disponível para venda
                title.textContent = 'Disponível na Loja';
                text.textContent = 'Este item está disponível para compra agora.';
                icon.className = 'bi bi-check-circle-fill';
                availability.className = 'item-availability status-disponivel';
                if (btnBuy) btnBuy.style.display = 'flex'; 
                if (btnDevolver) btnDevolver.style.display = 'none';
            } else {
                // Indisponível
                title.textContent = 'Indisponível';
                text.textContent = 'Este item não está disponível para compra.';
                icon.className = 'bi bi-x-circle-fill';
                availability.className = 'item-availability status-indisponivel';
                if (btnBuy) btnBuy.style.display = 'none';
                if (btnDevolver) btnDevolver.style.display = 'none';
            }
        }

        // 4. Informações textuais
        const itemName = modal.querySelector('#modal-item-name');
        if (itemName) itemName.textContent = this.sanitizarTexto(item.nome);
        
        const itemType = modal.querySelector('#modal-item-type');
        if (itemType) itemType.textContent = this.sanitizarTexto(item.tipo);
        
        const priceElement = modal.querySelector('#modal-item-price');
        const priceLabel = priceElement ? priceElement.nextElementSibling : null;

        if (item.preco !== null) {
            if (priceElement) priceElement.textContent = item.preco.toLocaleString('pt-BR');
            if (priceLabel) priceLabel.style.display = 'block';
        } else {
            if (priceElement) priceElement.textContent = 'N/A';
            if (priceLabel) priceLabel.style.display = 'none';
        }
        
        const itemDesc = modal.querySelector('#modal-item-description');
        if (itemDesc) itemDesc.textContent = this.sanitizarTexto(item.descricao || 'Sem descrição.');

        const detailRarity = modal.querySelector('#modal-detail-rarity');
        if (detailRarity) detailRarity.textContent = this.sanitizarTexto(item.raridade);
        
        const detailCategory = modal.querySelector('#modal-detail-category');
        if (detailCategory) detailCategory.textContent = this.sanitizarTexto(item.tipo);

        const detailDate = modal.querySelector('#modal-detail-date');
        if (detailDate) detailDate.textContent = this.formatarData(item.dataInclusao);
    }
    
    // --- Lógica de Compra e Devolução ---

    // Clique no botão comprar: Valida saldo e pede confirmação
    handleCompraClick() {
        if (!this.user) {
            Swal.fire({ icon: 'error', title: 'Login Necessário', text: 'Você precisa estar logado para fazer uma compra.' });
            return;
        }
        if (!this.currentItemInModal || !this.currentItemInModal.id) {
            Swal.fire({ icon: 'error', title: 'Erro', text: 'Nenhum item selecionado.' });
            return;
        }
        if (this.userData.creditos < this.currentItemInModal.preco) {
            Swal.fire({ icon: 'warning', title: 'Saldo Insuficiente', text: `Você não tem V-Bucks suficientes.` });
            return;
        }
        
        Swal.fire({
            title: 'Confirmar Compra?',
            html: `Você está prestes a comprar <b>${this.currentItemInModal.nome}</b> por <b>${this.currentItemInModal.preco.toLocaleString('pt-BR')} V-Bucks</b>.`,
            icon: 'question',
            showCancelButton: true,
            confirmButtonText: 'Confirmar',
            cancelButtonText: 'Cancelar',
        }).then((result) => {
            if (result.isConfirmed) {
                this.executarCompra(this.currentItemInModal.id);
            }
        });
    }

    // Executa a compra na API (/compra/{id})
    async executarCompra(itemId) {
        try {
            const response = await fetch(`${this.API_BASE_URL}/compra/${itemId}`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${this.user}`
                }
            });

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(errorText || 'Falha na transação.');
            }

            const successText = await response.text();
            console.log(successText); 

            await Swal.fire({
                title: 'Compra Realizada!',
                text: `O item "${this.currentItemInModal.nome}" foi adicionado à sua coleção.`,
                icon: 'success'
            });

            // Atualiza dados locais e UI após sucesso
            await this.buscaDadosusuario(); 
            this.atualizarStatusItemLocal(itemId, true); 

            this.preencherModalItem(this.currentItemInModal);
            this.renderizaItens(); 
            this.renderizarTodosOsItens(); 
            this.renderizarMeusItens(); 

        } catch (error) {
            console.error('Erro ao comprar item:', error);
            Swal.fire({ title: 'Erro na Compra', text: error.message, icon: 'error' });
        }
    }

    // Clique no botão devolver: Pede confirmação
    handleDevolucaoClick() {
        if (!this.user) {
            Swal.fire({ icon: 'error', title: 'Erro', text: 'Você precisa estar logado.' });
            return;
        }
        if (!this.currentItemInModal || !this.currentItemInModal.id) return;

        Swal.fire({
            title: 'Confirmar Devolução?',
            html: `Você está prestes a devolver <b>${this.currentItemInModal.nome}</b>. Esta ação não pode ser desfeita.`,
            icon: 'warning',
            showCancelButton: true,
            confirmButtonText: 'Confirmar Devolução',
            cancelButtonText: 'Cancelar',
            confirmButtonColor: '#dc3545', // Vermelho
        }).then((result) => {
            if (result.isConfirmed) {
                this.executarDevolucao(this.currentItemInModal.id);
            }
        });
    }

    // Executa a devolução na API (/devolucao/{id})
    async executarDevolucao(itemId) {
        try {
            const response = await fetch(`${this.API_BASE_URL}/devolucao/${itemId}`, { 
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${this.user}`
                }
            });

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(errorText || 'Falha na devolução.');
            }

            const successText = await response.text();
            console.log(successText); 

            await Swal.fire({
                title: 'Item Devolvido!',
                text: `O item "${this.currentItemInModal.nome}" foi removido da sua coleção.`,
                icon: 'success'
            });

            // Atualiza dados locais e UI após sucesso
            await this.buscaDadosusuario(); 
            this.atualizarStatusItemLocal(itemId, false); 

            this.preencherModalItem(this.currentItemInModal);
            this.renderizaItens(); 
            this.renderizarTodosOsItens();
            this.renderizarMeusItens(); 

        } catch (error) {
            console.error('Erro ao devolver item:', error);
            Swal.fire({ title: 'Erro na Devolução', text: error.message, icon: 'error' });
        }
    }

    // Atualiza estado local de um item (comprado/devolvido) sem recarregar tudo
    atualizarStatusItemLocal(itemId, isAdquiridoStatus) {
        // Atualiza o Set
        if (isAdquiridoStatus) {
            this.itensAdquiridosSet.add(itemId);
        } else {
            this.itensAdquiridosSet.delete(itemId);
        }
        
        // Atualiza arrays locais
        const itemNaLoja = this.itens.find(i => i.id === itemId);
        if (itemNaLoja) {
            itemNaLoja.isAdquirido = isAdquiridoStatus;
            if(this.currentItemInModal && this.currentItemInModal.id === itemId) {
                this.currentItemInModal = itemNaLoja;
            }
        }
        
        const itemEmTodos = this.todosOsItens.find(i => i.id === itemId);
        if (itemEmTodos) {
            itemEmTodos.isAdquirido = isAdquiridoStatus;
            if(this.currentItemInModal && this.currentItemInModal.id === itemId) {
                this.currentItemInModal = itemEmTodos;
            }
        }
    }

    // Formata data ISO para PT-BR
    formatarData(dataString) {
        if (!dataString) return 'N/A';
        try {
            const data = new Date(dataString);
            return data.toLocaleDateString('pt-BR', {
                day: '2-digit', month: '2-digit', year: 'numeric'
            });
        } catch (e) {
            return dataString;
        }
    }

    // Mapeia raridade (texto) para classe CSS
    obterClasseRaridade(raridade) {
        const mapaCoresRaridade = {
            'Lendário': 'legendary',
            'Épico': 'epic',
            'Raro': 'rare',
            'Incomum': 'uncommon',
            'Comum': 'common'
        };
        return mapaCoresRaridade[raridade] || 'serie';
    }

    // Sanitiza texto para evitar injeção de HTML (XSS)
    sanitizarTexto(texto) {
        const div = document.createElement('div');
        div.textContent = texto;
        return div.innerHTML;
    }

    // Valida URLs básicas
    sanitizarUrl(url) {
        try {
            new URL(url);
            return url;
        } catch {
            return '';
        }
    }

    // Renderiza mensagem de erro em um elemento
    mostrarErro(mensagem, gridElement = this.cosmeticosGrid) {
        if (gridElement) {
            gridElement.innerHTML = `
                <div class="col-12 d-flex justify-content-center">
                    <p class="text-center text-danger fs-5">${this.sanitizarTexto(mensagem)}</p>
                </div>
            `;
        }
    }
}

// -----------------------------------------------------------------
// CLASSE VALIDADORITEM
// -----------------------------------------------------------------

/**
 * Classe auxiliar para normalizar e validar dados de um item.
 * Garante que os campos tenham tipos corretos e valores default.
 */
class ValidadorItem {
    constructor(id, nome, tipo, raridade, preco, urlImagem, descricao, isNew, isForSale, isAdquirido, dataInclusao) {
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
    }

    // Retorna objeto limpo e seguro
    validaDados() {
        const nome = (typeof this.nome === 'string' && this.nome.trim() !== '') ? this.nome.trim() : 'Sem nome';
        const tipo = (typeof this.tipo === 'string' && this.tipo.trim() !== '') ? this.tipo.trim() : 'Cosmético';
        const raridade = (typeof this.raridade === 'string' && this.raridade.trim() !== '') ? this.raridade.trim() : 'Comum';
        const preco = (typeof this.preco === 'number' && this.preco >= 0) ? this.preco : null;
        const urlImagem = (typeof this.urlImagem === 'string') ? this.urlImagem : null;
        const descricao = (typeof this.descricao === 'string') ? this.descricao.trim() : '';
        const isNew = typeof this.isNew === 'boolean' ? this.isNew : false;
        const isForSale = typeof this.isForSale === 'boolean' ? this.isForSale : false;
        const isAdquirido = typeof this.isAdquirido === 'boolean' ? this.isAdquirido : false;
        const dataInclusao = this.dataInclusao;

        return {
            id: this.id,
            nome: nome,
            tipo: tipo,
            raridade: raridade,
            preco: preco,
            urlImagem: urlImagem,
            descricao: descricao,
            isNew: isNew,
            isForSale: isForSale,
            isAdquirido: isAdquirido,
            dataInclusao: dataInclusao
        };
    }
}