document.addEventListener('DOMContentLoaded', () => {
  const grid = document.getElementById('grid');
  const projectTitle = document.getElementById('project-title');

  // Load JSON data
  const loadProject = async () => {
    try {
      const response = await fetch('arkive-showcase.json');
      const data = await response.json();

      // Update the project title dynamically
      const projectName = data.projectName || 'Unknown Project';
      projectTitle.textContent = `Arkive Showcase for ${projectName}`;

      // Display modules
      const modules = data.modules || [];
      displayModules(modules);
    } catch (error) {
      console.error('Error loading JSON:', error);
    }
  };

  // Display modules in the grid
  const displayModules = (modules) => {
    grid.innerHTML = '';
    modules.forEach((module) => {
      const moduleCard = document.createElement('div');
      moduleCard.className = 'grid-item';
      moduleCard.innerHTML = `
        <h2>${module.name}</h2>
        <p>${module.items.length} Components</p>
      `;
      moduleCard.addEventListener('click', () => {
        window.location.href = `module.html?module=${module.name}`;
      });
      grid.appendChild(moduleCard);
    });
  };

  // Load project on page load
  loadProject();
});