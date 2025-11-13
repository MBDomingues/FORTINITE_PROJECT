/**
 * Classe responsável por gerenciar a autenticação (Login e Cadastro).
 * Controla a interface, validação de formulários e comunicação com a API.
 */
class UserPerfil {
    constructor() {
        // Definição dos endpoints da API
        this.API_LOGIN = 'http://130.213.12.104:8080/api/v1/auth/login';
        this.API_CADASTRO = 'http://130.213.12.104:8080/api/v1/auth/cadastro';
        
        // Inicialização das variáveis de estado do formulário
        this.email = '';
        this.password = '';
        this.name = '';
        this.confirmPassword = '';

        // Inicia a captura dos elementos do DOM
        this.buscaBotoes();
    }

    // Mapeia os botões e elementos de erro no HTML
    buscaBotoes() {
        this.loginButton = document.getElementById('btn-login');
        this.registerButton = document.getElementById('btn-cadastro');
        
        // Elemento para exibir erros na tela de login
        this.errorElement = document.getElementById('error-message'); 

        // Salva o texto original dos botões para restaurar após o "loading"
        if (this.loginButton) {
            this.loginButton.originalText = this.loginButton.innerHTML;
        }
        if (this.registerButton) {
            this.registerButton.originalText = this.registerButton.innerHTML;
        }

        this.eventosBotoes();
    }

    // Adiciona os ouvintes de evento (click) aos botões
    eventosBotoes() {
        // --- Configuração do Botão de Login ---
        if (this.loginButton){
            this.loginButton.addEventListener('click', (e) => {
                e.preventDefault(); // Impede o recarregamento da página
                
                // Captura e limpa os dados dos inputs
                this.email = document.getElementById('email').value.trim();
                this.password = document.getElementById('password').value.trim();
                
                // Validação básica
                if(!this.email || !this.password){
                    this.showLoginError('Email e senha são obrigatórios.');
                    return;
                }
                this.login(); // Inicia o processo de login
            });
        }

        // --- Configuração do Botão de Cadastro ---
        if (this.registerButton){
            this.registerButton.addEventListener('click', (e) => {
                e.preventDefault();
                
                // Captura e limpa os dados dos inputs
                this.email = document.getElementById('email').value.trim();
                this.password = document.getElementById('password').value.trim();
                this.name = document.getElementById('name').value.trim();
                this.confirmPassword = document.getElementById('confirm-password').value.trim();
                
                // Validação de campos vazios
                if(!this.email || !this.password || !this.name || !this.confirmPassword){
                    Swal.fire({
                        icon: 'warning',
                        title: 'Campos Vazios',
                        text: 'Por favor, preencha todos os campos.'
                    });
                    return;
                }
                this.register(); // Inicia o processo de cadastro
            });
        }
    }

    // Executa a lógica de cadastro de usuário
    async register() {
        this.setLoading(true, this.registerButton); // Ativa animação de carregamento

        // Validação de igualdade de senhas
        if (this.password !== this.confirmPassword) {
            Swal.fire({
                icon: 'error',
                title: 'Oops...',
                text: 'As senhas não coincidem.'
            });
            this.setLoading(false, this.registerButton);
            return;
        }

        const data = {
            email: this.email,
            senha: this.password,
            nome: this.name
        };

        try {
            // Requisição POST para a API de cadastro
            const response = await fetch(this.API_CADASTRO, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(data)
            });
            const resultText = await response.text();

            // Verifica se houve erro na requisição
            if (!response.ok) {
                throw new Error(resultText || `Erro HTTP! Status: ${response.status}`);
            }
            
            // Sucesso: Exibe alerta e redireciona para login
            await Swal.fire({
                icon: 'success',
                title: 'Cadastro realizado!',
                text: `${resultText} Você será redirecionado para o login.`,
                confirmButtonText: 'Entendido!'
            });
            
            window.location.href = 'login.html';

        } catch (error) {
            console.error('Falha no Cadastro:', error);
            // Exibe erro retornado pela API ou erro genérico
            Swal.fire({
                icon: 'error',
                title: 'Falha no Cadastro',
                text: error.message || 'Não foi possível conectar ao servidor.' 
            });
        } finally {
            this.setLoading(false, this.registerButton); // Restaura o botão
        }
    }

    // Executa a lógica de login do usuário
    async login() {
        this.setLoading(true, this.loginButton); // Ativa animação de carregamento

        const data = {
            email: this.email,
            senha: this.password
        };

        try {
            // Requisição POST para a API de login
            const response = await fetch(this.API_LOGIN, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(data)
            });

            // Tratamento de erros (tenta ler como JSON, se falhar lê como Texto)
            if (!response.ok) {
                let errorMsg = `Erro HTTP! Status: ${response.status}`;
                try {
                    const errorData = await response.json();
                    errorMsg = errorData.message || JSON.stringify(errorData);
                } catch (e) {
                    errorMsg = await response.text() || errorMsg;
                }
                throw new Error(errorMsg);
            }

            // Sucesso: Processa a resposta e salva o Token
            const responseData = await response.json();
            console.log('Success:', responseData);
            
            if (responseData.token) {
                localStorageManager.saveToken(responseData.token);
                window.location.href = 'index.html'; // Redireciona para a Home
            } else {
                throw new Error('Login bem-sucedido, mas nenhum token foi recebido.');
            }

        } catch (error) {
            console.error('Falha no Login:', error);
            this.showLoginError(error.message || 'Não foi possível conectar ao servidor.');
        
        } finally {
            this.setLoading(false, this.loginButton); // Restaura o botão
        }
    }

    /**
     * Controla o estado visual de carregamento dos botões (Loading Spinner)
     * @param {boolean} isLoading - Verdadeiro para mostrar spinner, falso para texto original
     * @param {HTMLElement} button - O botão a ser modificado
     */
    setLoading(isLoading, button) {
        if (!button) return;

        if (isLoading) {
            button.disabled = true;
            button.innerHTML = '<span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span> Carregando...';
            
            // Limpa mensagens de erro anteriores
            if (this.errorElement) {
                this.errorElement.textContent = '';
                this.errorElement.style.display = 'none';
            }
        } else {
            button.disabled = false;
            button.innerHTML = button.originalText; // Restaura texto original
        }
    }

    // Exibe mensagens de erro na interface de login (HTML)
    showLoginError(message) {
        if (!this.errorElement) return;
        
        const alertBox = document.getElementById('alert-error'); 
        
        this.errorElement.textContent = message;
        if (alertBox) {
            alertBox.classList.remove('d-none'); // Torna o alerta visível
        }
    }
}

/**
 * Classe utilitária estática para gerenciar o Token JWT no LocalStorage.
 */
class localStorageManager {
    constructor() {} 

    // Salva o token no navegador
    static saveToken(token) {
        localStorage.setItem('jwt_token', token);
    }
    // Recupera o token salvo
    static getToken() {
        return localStorage.getItem('jwt_token');
    }
    // Remove o token (Logout)
    static removeToken() {
        localStorage.removeItem('jwt_token');
    }   
    // Verifica se o usuário possui um token
    static isLoggedIn() {
        return !!this.getToken();
    }
}