# Contraponto

Plataforma de publicação de artigos com foco em escrita de qualidade, experiência de leitura imersiva e gerenciamento de conteúdo.

## ✨ Funcionalidades

- **Autenticação de usuários** – Cadastro, login, logout, perfil de usuário (com avatar via UI Avatars).
- **Editor de artigos** – Editor WYSIWYG com suporte a Markdown, toolbar para formatação, slug personalizado, descrição resumida.
- **Publicação e rascunhos** – Salvar como rascunho, publicar, editar posts já publicados.
- **Biblioteca pessoal** – Lista organizada em abas (Rascunhos / Publicados) com ações de edição e exclusão (apenas rascunhos).
- **Busca** – Busca em tempo real via modal ou página dedicada, pesquisa por título, descrição ou conteúdo.
- **Página inicial** – Exibe posts publicados com destaque para o mais recente, grid responsivo.
- **Upload de imagens** – Suporte a JPEG, PNG, GIF, WebP, redimensionamento e cache.
- **HTMX** – Navegação sem recarregamento total da página, trocas parciais de conteúdo, requisições assíncronas.
- **Design responsivo** – Adaptado para desktop, tablet e dispositivos móveis com tipografia elegante.

## 🧱 Tecnologias

| Camada          | Tecnologias                                                                 |
|-----------------|-----------------------------------------------------------------------------|
| Backend         | Java 17+, Quarkus, RESTEasy Reactive, Hibernate ORM, CDI, Qute Templates    |
| Frontend        | HTMX, CSS puro (design system customizado), JavaScript (validação, toasts) |
| Banco de dados  | PostgreSQL (ou qualquer banco relacional via Hibernate)                    |
| Autenticação    | Sessão gerenciada em memória (custom `LoggedUserProvider`), JCrypt para hash |
| Imagens         | Upload para sistema de arquivos, geração automática de URLs                |
| Build           | Maven, Quarkus Dev Services (para desenvolvimento)                         |

## 📦 Pré‑requisitos

- Java 17 ou superior
- Maven 3.8+
- PostgreSQL (ou configure outro banco no `application.properties`)
- (Opcional) Docker – para rodar o banco via container

## 🚀 Instalação e execução

### Desenvolvimento (modo dev)

```bash
# Clonar o repositório
git clone https://github.com/vepo/contraponto.git
cd contraponto

# Executar com Quarkus Dev Mode
./mvnw quarkus:dev
```

A aplicação estará disponível em `http://localhost:8080`.  
O modo dev oferece live reload, console Dev UI e configuração automática do banco via Dev Services (se não houver configuração explícita).

### Produção

1. **Build**:
   ```bash
   ./mvnw clean package -Pprod
   ```

2. **Executar**:
   ```bash
   java -jar target/quarkus-app/quarkus-run.jar
   ```

   Ou use a imagem Docker gerada:
   ```bash
   docker build -f src/main/docker/Dockerfile.jvm -t contraponto .
   docker run -i --rm -p 8080:8080 contraponto
   ```

### Configuração do banco de dados

Crie um arquivo `application.properties` (ou use variáveis de ambiente):

```properties
quarkus.datasource.db-kind=postgresql
quarkus.datasource.username=meuusuario
quarkus.datasource.password=minhasenha
quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/contraponto

quarkus.hibernate-orm.database.generation=update
```

O schema será criado automaticamente na primeira execução (`update`).

## 📂 Estrutura de diretórios (relevante)

```
src/main/
├── java/dev/vepo/contraponto/
│   ├── auth/               # Serviço de hash de senha (BCrypt)
│   ├── components/         # Endpoints de componentes reutilizáveis (menu, modal de auth)
│   ├── home/               # Página inicial
│   ├── image/              # Upload, armazenamento e recuperação de imagens
│   ├── library/            # Página "Minha Biblioteca" (rascunhos + publicados)
│   ├── post/               # Entidade Post, repositório, endpoints de visualização
│   ├── search/             # Busca (página e modal)
│   ├── shared/             # Infraestrutura: LoggedUser, filtros, extensões Qute
│   ├── user/               # Entidade User, repositório
│   └── write/              # Editor de artigos
├── resources/
│   ├── templates/          # Templates Qute (.html)
│   ├── META-INF/resources/ # Arquivos estáticos (CSS, JS, imagens)
│   └── application.properties
```

## 🔐 Autenticação

- O login/signup é feito via forms HTML + HTMX.
- Após autenticação, um cookie `__session` é definido e uma sessão é mantida em memória (`LoggedUserProvider`).
- A anotação `@Logged` em endpoints exige usuário autenticado (redireciona para home se não estiver).
- O componente de menu é atualizado via OOB swap após login/logout.
- A senha é armazenada usando BCrypt.

## 🖼️ Upload de imagens

- Endpoint: `POST /api/images` (multipart/form-data).
- As imagens são salvas no diretório configurado (`image.storage.path`).
- O nome do arquivo é renomeado para UUID + extensão original.
- A URL de acesso é `/api/images/{uuid}`.
- As imagens são cacheadas por 1 ano (Cache-Control).

## 📝 Editor de artigos

- Utiliza `<textarea>` com toolbar JavaScript que insere sintaxe Markdown.
- Suporte a negrito, itálico, títulos, listas, citações, código e links.
- O formulário envia dados para `/forms/write/draft` (salvar rascunho) ou `/forms/write/publish` (publicar).
- Após publicação, redireciona para a página do post.

## 🔍 Busca

- **Modal**: acionada pelo ícone de lupa no cabeçalho. Mostra resultados em tempo real conforme digitação.
- **Página dedicada**: acessível via `/search`. Permite paginação e visualização completa dos resultados.
- A busca é feita no backend via HQL, pesquisando em título, descrição e conteúdo de posts publicados.

## 🎨 Estilos e JavaScript

- Arquivos CSS:
  - `main.css` – sistema de design, componentes gerais, responsividade.
  - `write.css` – estilos específicos da página de escrita.
  - `fonts.css` – fontes auto-hospedadas (Cormorant Garamond, Inter, Playfair Display).
- Scripts JS (em `META-INF/resources/js`):
  - `main.js` – gerencia proteção de rotas, organização de elementos.
  - `forms.js` – validação de formulários (pristine, mensagens de erro).
  - `authentication.js` – injeta token JWT (se usado – atualmente não essencial).
  - `header.js` – controla dropdown do menu do usuário.
  - `write.js` – toolbar do editor.
  - `toast.js` – notificações via cabeçalhos HTTP (`X-Toast-Message`).

## 🧪 Testes (sugestão)

A aplicação não possui testes automatizados no momento, mas é possível adicionar testes de integração com `@QuarkusTest` e `RestAssured`. Contribuições são bem‑vindas.

## 🤝 Contribuindo

1. Faça um fork do projeto.
2. Crie uma branch para sua feature (`git checkout -b feature/nova-feature`).
3. Commit suas mudanças (`git commit -m 'Adiciona nova feature'`).
4. Push para a branch (`git push origin feature/nova-feature`).
5. Abra um Pull Request.

## 📄 Licença

Este projeto está sob a licença MIT. Consulte o arquivo [LICENSE](LICENSE) para mais informações.

---

Desenvolvido com ❤️ e ☕ por [Victor Osório](https://github.com/vepo).